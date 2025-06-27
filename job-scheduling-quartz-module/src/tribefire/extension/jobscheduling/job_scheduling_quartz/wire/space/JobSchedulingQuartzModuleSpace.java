// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
package tribefire.extension.jobscheduling.job_scheduling_quartz.wire.space;

import static com.braintribe.wire.api.scope.InstanceConfiguration.currentInstance;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

import com.braintribe.exception.Exceptions;
import com.braintribe.logging.Logger;
import com.braintribe.model.processing.deployment.api.ExpertContext;
import com.braintribe.model.processing.deployment.api.binding.DenotationBindingBuilder;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import tribefire.extension.job_scheduling.deployment.model.JobCronScheduling;
import tribefire.extension.job_scheduling.processing.QuartzScheduling;
import tribefire.module.wire.contract.TribefireModuleContract;
import tribefire.module.wire.contract.TribefireWebPlatformContract;

/**
 * This module's javadoc is yet to be written.
 */
@Managed
public class JobSchedulingQuartzModuleSpace implements TribefireModuleContract {

	private static final Logger log = Logger.getLogger(JobSchedulingQuartzModuleSpace.class);

	@Import
	private TribefireWebPlatformContract tfPlatform;

	@Override
	public void bindDeployables(DenotationBindingBuilder bindings) {
		bindings.bind(JobCronScheduling.T) //
				.component(tfPlatform.binders().worker()) //
				.expertFactory(this::quartzScheduling);

	}

	@Managed
	private QuartzScheduling quartzScheduling(ExpertContext<JobCronScheduling> context) {
		QuartzScheduling bean = new QuartzScheduling();
		bean.setDeployable(context.getDeployable());
		bean.setRequestEvaluator(tfPlatform.requestUserRelated().evaluator());
		bean.setUserSessionScoping(tfPlatform.masterUserAuthContext().userSessionScoping());
		bean.setScheduler(scheduler());
		return bean;
	}

	@Managed
	private Scheduler scheduler() {
		try {
			Scheduler bean = StdSchedulerFactory.getDefaultScheduler();

			currentInstance().onDestroy(() -> {
				try {
					bean.shutdown(true);
				} catch (SchedulerException e) {
					log.error("error while shutting down standard quartz scheduler", e);
				}
			});

			bean.start();
			return bean;

		} catch (SchedulerException e) {
			throw Exceptions.unchecked(e, "Error while creating quartz standard scheduler");
		}
	}
}

package io.jenkins.plugins.freestyle.steps;

import java.io.IOException;
import java.io.Serializable;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import io.jenkins.plugins.config.DevOpsJobProperty;
import io.jenkins.plugins.model.DevOpsModel;
import io.jenkins.plugins.pipeline.steps.executions.DevOpsPipelineMapStepExecution;
import io.jenkins.plugins.utils.DevOpsConstants;
import io.jenkins.plugins.utils.GenericUtils;
import jenkins.tasks.SimpleBuildStep;

/**
 * Register Artifact build step.
 * Can be configured on free-style job.
 * 
 * @author prashanth.pedduri
 *
 */
public class DevOpsRegisterArtifactBuildStep extends Builder implements SimpleBuildStep, Serializable{

	private static final long serialVersionUID = 1L;
	
	private String artifactsPayload;
	
	public DevOpsRegisterArtifactBuildStep() {
		super();
	}
	
	@DataBoundConstructor
	public DevOpsRegisterArtifactBuildStep(String artifactsPayload) {
		this.artifactsPayload = artifactsPayload;
	}
	
	public String getArtifactsPayload() {
		return artifactsPayload;
	}
	
	@DataBoundSetter
	public void setArtifactsPayload(String artifactsPayload) {
		this.artifactsPayload = artifactsPayload;
	}
	
	public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener, EnvVars envVars)
			throws InterruptedException, IOException {
		
		if(envVars == null)
			envVars = GenericUtils.getEnvVars(run, listener);
		
		DevOpsModel model = new DevOpsModel();
		DevOpsJobProperty jobProperties = model.getJobProperty(run.getParent());
		
		// Resolve payload (to replace env/system variables)
		String expandedPayload = envVars.expand(this.artifactsPayload);

		String _result = model.handleArtifactRegistration(run, listener, expandedPayload, envVars);
		
		printDebug("perform", new String[] { "message" }, new String[] { "handleArtifactRegistration responded with: "+_result },
				model.isDebug());
		
		if (null != _result && !_result.contains(DevOpsConstants.COMMON_RESULT_FAILURE.toString())) {
			GenericUtils.printConsoleLog(listener, "SUCCESS: Register Artifact request was successful.");
			printDebug("perform", new String[] { "message" }, new String[] { "SUCCESS: Register Artifact request was successful." },
					model.isDebug());
		} else {
			String errorMsg = "FAILED: Artifact could not be registered.";
			if(null != _result)
				errorMsg += "Cause: "+_result;
			printDebug("perform", new String[] { "message" }, new String[] { errorMsg },
					model.isDebug());
			GenericUtils.printConsoleLog(listener, errorMsg);
			if (jobProperties != null && jobProperties.isIgnoreSNErrors()) {
				GenericUtils.printConsoleLog(listener, "IGNORED: Artifact registration error ignored.");
			} else { // TODO: Should we really abort here?
				throw new AbortException(errorMsg);
			}
		}
	}
	
	@Override
	public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
			throws InterruptedException, IOException {
		
		perform(run, workspace, launcher, listener, null);
	}
	
	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	
	@Extension
	@Symbol("snDevOpsArtifact")
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
		
		public DescriptorImpl() {
			load();
		}
		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}
		
		@Override
		public String getDisplayName() {
			return DevOpsConstants.ARTIFACT_REGISTER_STEP_DISPLAY_NAME.toString();
		}
	}
	
	private void printDebug(String methodName, String[] variables, String[] values, boolean debug) {
		GenericUtils.printDebug(DevOpsPipelineMapStepExecution.class.getName(), methodName, variables, values, debug);
	}
	
}
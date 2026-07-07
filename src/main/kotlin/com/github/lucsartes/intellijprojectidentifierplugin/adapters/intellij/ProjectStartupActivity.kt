package com.github.lucsartes.intellijprojectidentifierplugin.adapters.intellij

import com.github.lucsartes.intellijprojectidentifierplugin.core.ApplicationSettings
import com.github.lucsartes.intellijprojectidentifierplugin.core.PluginSettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/**
 * Entry point on project open. Runs the watermark pipeline once, wires branch watching, and re-runs
 * the pipeline when project-level or application-level settings change. All heavy lifting lives in
 * [WatermarkPipelineService]; this activity only orchestrates the triggers.
 */
class ProjectStartupActivity : ProjectActivity {

    private val log = Logger.getInstance(ProjectStartupActivity::class.java)

    override suspend fun execute(project: Project) {
        log.info("Starting Project Identifier pipeline for '${project.name}' on startup")

        val pipeline = project.getService(WatermarkPipelineService::class.java)
        val branchWatch = project.getService(BranchWatchService::class.java)

        // Initial render, then start branch watching if the override uses ${branch}.
        pipeline.rerun()
        branchWatch.sync()

        // Tie both subscriptions to the pipeline service's lifetime so they are disposed on project close.
        project.messageBus.connect(pipeline).subscribe(
            IntelliJSettingsService.TOPIC,
            object : IntelliJSettingsService.SettingsChangedListener {
                override fun settingsChanged(newSettings: PluginSettings) {
                    log.info("Project settings changed for '${project.name}'; refreshing")
                    branchWatch.sync()
                    pipeline.rerun()
                }
            },
        )

        ApplicationManager.getApplication().messageBus.connect(pipeline).subscribe(
            IntelliJApplicationSettingsService.TOPIC,
            object : IntelliJApplicationSettingsService.SettingsChangedListener {
                override fun settingsChanged(newSettings: ApplicationSettings) {
                    log.info("Application settings changed; refreshing '${project.name}'")
                    pipeline.rerun()
                }
            },
        )

        log.info("Project Identifier wiring complete for '${project.name}'")
    }
}

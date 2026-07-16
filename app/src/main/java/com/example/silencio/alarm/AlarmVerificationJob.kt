package com.example.silencio.alarm

import android.annotation.SuppressLint
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.example.silencio.data.repository.SilencioRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
@SuppressLint("SpecifyJobSchedulerIdRange")
class AlarmVerificationJob : JobService() {

    @Inject
    lateinit var repository: SilencioRepository

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.d("AlarmVerificationJob", "Job fired — verifying alarms")

        scope.launch {
            val isOnboarded = repository.isOnboarded.first()
            if (isOnboarded) {
                repository.getUpcomingMeetings()
                Log.d("AlarmVerificationJob", "Alarms verified and rescheduled if needed")
            }
            jobFinished(params, false)
        }

        return true // async work in progress
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        return true // reschedule if stopped early
    }

    companion object {
        private const val JOB_ID = 1001

        fun schedule(context: Context) {
            val scheduler = context.getSystemService(JobScheduler::class.java)

            // Don't reschedule if already running
            if (scheduler.getPendingJob(JOB_ID) != null) return

            val job = JobInfo.Builder(
                JOB_ID,
                ComponentName(context, AlarmVerificationJob::class.java)
            )
                .setPeriodic(15 * 60 * 1000L) // every 15 minutes
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
                .setPersisted(true) // survives reboot
                .build()

            scheduler.schedule(job)
            Log.d("AlarmVerificationJob", "Job scheduled")
        }
    }
}
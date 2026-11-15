package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class JarvisRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun userProfileAndTaskCrud_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser("alice_${UUID.randomUUID()}@test.com")
        val user = auth.currentUser!!
        val repo = JarvisRepository(firestore)

        val profileRes = withTimeout(TIMEOUT_MS) { repo.ensureUserProfile(user) }
        assertTrue(profileRes.isSuccess)
        assertEquals("free", profileRes.getOrThrow().plan)

        val taskRes = withTimeout(TIMEOUT_MS) {
            repo.addTask(uid, "Calibrate Arc Core", "Verify telemetry", "high", "Today")
        }
        assertTrue(taskRes.isSuccess)
        val taskId = taskRes.getOrThrow()

        val tasks = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeTasks(uid).first { list -> list.any { it.id == taskId } }
        }
        assertTrue(tasks.any { it.id == taskId && it.title == "Calibrate Arc Core" })

        val noteRes = withTimeout(TIMEOUT_MS) {
            repo.addNote(uid, "Architecture Note", "Modular compose UI", "Dev")
        }
        assertTrue(noteRes.isSuccess)
    }

    @Test
    fun crossUserRead_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser("alice_iso_${UUID.randomUUID()}@test.com")
        val aliceRepo = JarvisRepository(firestore)
        withTimeout(TIMEOUT_MS) {
            aliceRepo.ensureUserProfile(auth.currentUser!!)
            aliceRepo.addTask(aliceUid, "Secret Task")
        }

        signInTestUser("bob_iso_${UUID.randomUUID()}@test.com")
        val bobRepo = JarvisRepository(firestore)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeTasks(aliceUid).first()
            }
        } catch (t: Throwable) {
            caught = t
        }
        val fsEx = caught?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    @Test
    fun unauthenticatedObserve_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = JarvisRepository(firestore)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeTasks("some_uid_123").first()
            }
        } catch (t: Throwable) {
            caught = t
        }
        val fsEx = caught?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    private companion object {
        const val TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}

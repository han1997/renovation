package com.renovation.guardian.ui.nav

import android.app.Application
import android.net.Uri
import org.robolectric.Shadows.shadowOf
import java.io.ByteArrayOutputStream
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class FileActionsViewModelTest {
    @Test fun exportReportsSuccessOnlyAfterTheStreamHasBeenWritten() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val app = ApplicationProvider.getApplicationContext<Application>()
        val state = SavedStateHandle()
        val vm = FileActionsViewModel(app, state)
        val store = ViewModelStore().apply { put("files", vm) }
        try {
            val uri = Uri.parse("content://test.documents/backup.json")
            val output = object : ByteArrayOutputStream() {
                var closed = false
                override fun close() { super.close(); closed = true }
            }
            // AndroidX FileProvider 的根校验硬编码 '/'，Windows JVM 不能模拟该路径。
            // 此处验证 ContentResolver 流生命周期，真实 FileProvider 由仪器测试验证。
            shadowOf(app.contentResolver).registerOutputStream(uri, output)
            vm.export("json", "backup.json", "精确金额 0.29")
            assertEquals("json", vm.requests.first().kind)
            val temporary = File(state.get<String>("path")!!)
            assertTrue(temporary.exists())
            vm.finish(uri)
            assertTrue(vm.results.first() is FileActionResult.Success)
            advanceUntilIdle()
            assertEquals("精确金额 0.29", output.toString("UTF-8"))
            assertTrue(output.closed)
            assertFalse(temporary.exists())
        } finally { store.clear(); Dispatchers.resetMain() }
    }
    @Test fun cancelAndReadWriteFailureAreDistinctResults() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = FileActionsViewModel(app, SavedStateHandle())
        val store = ViewModelStore().apply { put("files", vm) }
        try {
            vm.export("csv", "budget.csv", "data")
            vm.requests.first(); vm.finish(null)
            assertEquals(FileActionResult.Cancelled, vm.results.first())
            advanceUntilIdle()
            vm.export("json", "backup.json", "data")
            vm.requests.first(); vm.finish(Uri.parse("content://missing.provider/file"))
            assertTrue(vm.results.first() is FileActionResult.Failure)
        } finally { store.clear(); Dispatchers.resetMain() }
    }
}

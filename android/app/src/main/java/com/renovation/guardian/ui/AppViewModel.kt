package com.renovation.guardian.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.renovation.guardian.RenovationApp
import com.renovation.guardian.data.repo.AppContainer

/**
 * 所有屏幕 ViewModel 的基类：从 [RenovationApp] 拿到仓库容器，
 * 避免在各处重复强转 [android.content.Context] 为 [RenovationApp]。
 */
abstract class AppViewModel(application: Application) : AndroidViewModel(application) {
    protected val container: AppContainer by lazy {
        (application as RenovationApp).container
    }
}

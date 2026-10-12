package com.russelldickenson.lelauncher.interfaces

import com.russelldickenson.lelauncher.models.AppWidget

interface WidgetsFragmentListener {
    fun onWidgetLongPressed(appWidget: AppWidget)
}

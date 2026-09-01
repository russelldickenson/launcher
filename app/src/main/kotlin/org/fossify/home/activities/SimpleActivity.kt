package org.fossify.home.activities

import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.home.R
import org.fossify.home.helpers.REPOSITORY_NAME

open class SimpleActivity : BaseSimpleActivity() {
    override fun getAppIconIDs() = arrayListOf(R.mipmap.ic_launcher)

    override fun getAppLauncherName() = getString(R.string.app_launcher_name)

    override fun getRepositoryName() = REPOSITORY_NAME
}

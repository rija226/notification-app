package hr.podsjetnik

import android.app.Application
import android.content.Context
import hr.podsjetnik.reminders.NotificationFactory

class PodsjetnikApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationFactory.createChannels(this)
    }
}

val Context.container: AppContainer
    get() = (applicationContext as PodsjetnikApp).container

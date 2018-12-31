package org.ro.handler

import kotlinx.serialization.json.JsonObject
import org.ro.URLS
import org.ro.core.Globals
import org.ro.core.Utils
import org.ro.core.event.EventLog
import org.ro.core.event.LogEntry
import org.ro.core.model.ObjectList
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TObjectHandlerTest {
    private var spock = Globals
    private var dsp = Dispatcher

    @Test
    fun testService() {
        // given
        var ol = ObjectList()
        // when
        val jsonObj0 = JSON.parse<JsonObject>(URLS.SO_LIST_ALL)
        var le0: LogEntry = createLogEntry(jsonObj0)
        dsp.handle(le0)
        val jsonObj1 = JSON.parse<JsonObject>(URLS.SO_0)
        var le1: LogEntry = createLogEntry(jsonObj1)
        dsp.handle(le1)
        // then
        assertNotNull(ol)
        assertTrue(ol.length() == 0)
        //After SO_LIST_ALL is invoked, a couple of other URL's (3 top menu items) are invoked automatically.
        // therefore the expected number is 5 (for SimpleApp and not 2!
        assertTrue(5 == EventLog.getEntries()!!.size)
    }

    private fun createLogEntry(json: JsonObject): LogEntry {
        val url: String? = Utils().getSelfHref(json)
        val resp: String = JSON.stringify(json)
        val le: LogEntry = EventLog.start(url!!, "", null)
        le.response = resp
        return le
    }

}


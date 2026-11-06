package com.firstlogistics.flproof

import com.firstlogistics.flproof.utils.SeabankLabelParser
import org.junit.Assert.assertEquals
import org.junit.Test

class SeabankLabelParserTest {

    @Test
    fun testUserUploadedLabel() {
        val raw = """
            [CONFIDENTIAL]
            Kepada Yth. ELBA LIA
            Di alamat :
            SUMATERA SELATAN KABUPATEN OGAN KOMERING ILIR CENGAL DUSUN I
            RT 002/RW 002
        """.trimIndent()

        val parsed = SeabankLabelParser.extractFields(raw)
        println("Recipient: [${parsed.recipient}]")
        println("Address: [${parsed.address}]")
        println("CleanText:\n${parsed.cleanText}")

        assertEquals("ELBA LIA", parsed.recipient)
        val expectedAddress = "SUMATERA SELATAN KABUPATEN OGAN KOMERING ILIR CENGAL DUSUN I\nRT 002/RW 002"
        assertEquals(expectedAddress, parsed.address)
    }
}


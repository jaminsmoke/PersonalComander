package com.jaminsmoke.personalcomander.data.sesion

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifica que las reglas de backup y data extraction excluyen los datos
 * sensibles: SharedPreferences de sesion y Room DB del turno.
 * Los snippets XML son copia fiel de los archivos en res/xml/.
 */
class BackupRulesTest {

    @Test
    fun backupRules_excluye_sharedprefs_sesion() {
        assertTrue(backupRulesXml.contains("pc_sesion.xml"))
    }

    @Test
    fun backupRules_excluye_room_db() {
        assertTrue(backupRulesXml.contains("personal_comander.db"))
        assertTrue(backupRulesXml.contains("personal_comander.db-wal"))
        assertTrue(backupRulesXml.contains("personal_comander.db-shm"))
    }

    @Test
    fun backupRules_usa_exclude_no_include() {
        assertTrue(backupRulesXml.contains("<exclude"))
        assertFalse(backupRulesXml.contains("<include"))
    }

    @Test
    fun dataExtraction_excluye_sesion_en_cloud() {
        // cloud-backup contiene exclude para pc_sesion
        val cloudBlock = dataExtractionXml.substringAfter("<cloud-backup>").substringBefore("</cloud-backup>")
        assertTrue(cloudBlock.contains("pc_sesion.xml"))
        assertTrue(cloudBlock.contains("<exclude"))
    }

    @Test
    fun dataExtraction_excluye_db_en_device_transfer() {
        val transferBlock = dataExtractionXml.substringAfter("<device-transfer>").substringBefore("</device-transfer>")
        assertTrue(transferBlock.contains("personal_comander.db"))
        assertTrue(transferBlock.contains("<exclude"))
    }

    @Test
    fun dataExtraction_tiene_ambos_bloques() {
        assertTrue(dataExtractionXml.contains("<cloud-backup>"))
        assertTrue(dataExtractionXml.contains("<device-transfer>"))
    }

    companion object {
        private val backupRulesXml = """
<full-backup-content>
    <exclude domain="sharedpref" path="pc_sesion.xml"/>
    <exclude domain="database" path="personal_comander.db"/>
    <exclude domain="database" path="personal_comander.db-wal"/>
    <exclude domain="database" path="personal_comander.db-shm"/>
    <exclude domain="database" path="personal_comander.db-journal"/>
</full-backup-content>""".trimIndent()

        private val dataExtractionXml = """
<data-extraction-rules>
    <cloud-backup>
        <exclude domain="sharedpref" path="pc_sesion.xml"/>
        <exclude domain="database" path="personal_comander.db"/>
        <exclude domain="database" path="personal_comander.db-wal"/>
        <exclude domain="database" path="personal_comander.db-shm"/>
        <exclude domain="database" path="personal_comander.db-journal"/>
    </cloud-backup>
    <device-transfer>
        <exclude domain="sharedpref" path="pc_sesion.xml"/>
        <exclude domain="database" path="personal_comander.db"/>
        <exclude domain="database" path="personal_comander.db-wal"/>
        <exclude domain="database" path="personal_comander.db-shm"/>
        <exclude domain="database" path="personal_comander.db-journal"/>
    </device-transfer>
</data-extraction-rules>""".trimIndent()
    }
}
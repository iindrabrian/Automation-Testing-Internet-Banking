import static com.kms.katalon.core.checkpoint.CheckpointFactory.findCheckpoint
import static com.kms.katalon.core.testcase.TestCaseFactory.findTestCase
import static com.kms.katalon.core.testdata.TestDataFactory.findTestData
import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import static com.kms.katalon.core.testobject.ObjectRepository.findWindowsObject
import com.kms.katalon.core.checkpoint.Checkpoint as Checkpoint
import com.kms.katalon.core.cucumber.keyword.CucumberBuiltinKeywords as CucumberKW
import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.testcase.TestCase as TestCase
import com.kms.katalon.core.testdata.TestData as TestData
import com.kms.katalon.core.testng.keyword.TestNGBuiltinKeywords as TestNGKW
import com.kms.katalon.core.testobject.TestObject as TestObject
import com.kms.katalon.core.webservice.keyword.WSBuiltInKeywords as WS
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.windows.keyword.WindowsBuiltinKeywords as Windows
import internal.GlobalVariable as GlobalVariable
import org.openqa.selenium.Keys as Keys
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.model.FailureHandling
import javax.swing.JOptionPane

String inputCaptchaManually(TestObject captchaField, int timeout = 30) {
	try {
		// 1. Wait untuk captcha field muncul
		WebUI.waitForElementClickable(captchaField, timeout)
		WebUI.scrollToElement(captchaField, 10)
		
		// 2. Screenshot captcha untuk membantu user melihat
		WebUI.takeScreenshot("captcha_screenshot.png")
		
		// 3. Tampilkan popup untuk input captcha
		String captchaCode = JOptionPane.showInputDialog(
			null,
			"Masukkan kode CAPTCHA yang muncul di gambar:\n\n" +
			"(Screenshot disimpan di: Reports/captcha_screenshot.png)",
			"Input CAPTCHA - Bank Sumsel Babel",
			JOptionPane.QUESTION_MESSAGE
		)
		
		// 4. Validasi input tidak kosong
		if (captchaCode == null || captchaCode.trim().isEmpty()) {
			throw new Exception("CAPTCHA tidak boleh kosong!")
		}
		
		// 5. Set captcha ke form
		WebUI.setText(captchaField, captchaCode.trim())
		
		WebUI.comment("✓ CAPTCHA berhasil diinput: ${captchaCode}")
		
		return captchaCode.trim()
		
	} catch (Exception e) {
		WebUI.takeScreenshot("ERROR_InputCaptcha")
		throw new Exception("Gagal input CAPTCHA: ${e.getMessage()}")
	}
}



WebUI.openBrowser('')

WebUI.navigateToUrl('http://139.255.78.230:12080/corp/index.html')



//<login>
def inputField = findTestObject('Object Repository/m_bsb_lain/Page_Bank Sumsel Babel - Internet Banking/input_Masukkan ID Perusahaan Anda_loginCorp_6066dd')
	// Wait sampai visible dan clickable
WebUI.waitForElementVisible(inputField, 30)
WebUI.waitForElementClickable(inputField, 30)
	// Set text
WebUI.setText(inputField, 'RKU0862C')

WebUI.setText(findTestObject('Object Repository/m_bsb_lain/Page_Bank Sumsel Babel - Internet Banking/input_Silakan masukkan User ID anda_loginCo_3c7140'), 
    'MAKER0001')

	//<popup_captcha>
def captchaField = findTestObject('Object Repository/m_bsb_lain/Page_Bank Sumsel Babel - Internet Banking/input_Ketik code yang muncul digambar_login_fc3d9b')
String captchaCode = inputCaptchaManually(captchaField, 30)
	//</popup_captcha>

WebUI.sendKeys(findTestObject('Object Repository/m_bsb_lain/Page_Bank Sumsel Babel - Internet Banking/input_Ketik code yang muncul digambar_login_fc3d9b'), 
    Keys.chord(Keys.ENTER))

WebUI.setEncryptedText(findTestObject('Object Repository/m_bsb_lain/Page_Bank Sumsel Babel - Internet Banking/input_Silakan masukkan password anda_textfield'), 
    '7OEX3bziT2+5Gr57kyA9WA==')

WebUI.sendKeys(findTestObject('Object Repository/m_bsb_lain/Page_Bank Sumsel Babel - Internet Banking/input_Silakan masukkan password anda_textfield'), 
    Keys.chord(Keys.ENTER))

if (WebUI.verifyTextPresent('User ID atau Password Anda salah', false, FailureHandling.OPTIONAL)) {
    WebUI.comment('Login gagal - User ID atau Password salah, coba ulang')
    WebUI.refresh()
    WebUI.waitForPageLoad(30)
}
//</login>

// ========================================
// SCRIPT TRANSFER KE REKENING BSB LAIN
// Dengan Expected vs Actual Status Validation
// ========================================

// Load semua data dari database
// Load semua data dari database
def allData = findTestData('Data Files/data_bsb_lain')

// Hitung jumlah transaksi yang valid (yang punya transaksi_id)
int totalRows = 0
for (int i = 1; i <= allData.getRowNumbers(); i++) {
    def transaksi_id = allData.getValue('transaksi_id', i)
    if (transaksi_id != null && !transaksi_id.toString().trim().isEmpty()) {
        totalRows++
    } else {
        break
    }
}

WebUI.comment("Total transaksi: ${totalRows}")

// Statistics tracking
int totalProcessed = 0
int expectedPassed = 0
int expectedFailed = 0
int actualPassed = 0
int actualFailed = 0
int matchCount = 0
int mismatchCount = 0
boolean userStoppedExecution = false

// Loop berdasarkan jumlah transaksi yang valid
for (int i = 1; i <= totalRows; i++) {
    WebUI.comment("=== Processing transaksi ${i}/${totalRows} ===")
    
    def transaksi_id = allData.getValue('transaksi_id', i)
    def rek_sumber = allData.getValue('rek_sumber', i)
    def rek_tujuan = allData.getValue('rek_tujuan', i)
    def sejumlah = allData.getValue('sejumlah', i)
    def catatan = allData.getValue('catatan', i)
    def expectedStatus = allData.getValue('status', i)?.toString()?.toLowerCase()?.trim()
    
    // Validasi data wajib - jika tidak valid, skip ke transaksi berikutnya
    if (rek_sumber == null || rek_sumber.toString().trim().isEmpty() ||
        rek_tujuan == null || rek_tujuan.toString().trim().isEmpty() ||
        sejumlah == null || sejumlah.toString().trim().isEmpty()) {
        WebUI.comment("⚠ Transaksi ${transaksi_id} invalid (data tidak lengkap) - Skip")
        continue
    }
    
    // Validasi expected status
    if (expectedStatus == null || expectedStatus.isEmpty()) {
        WebUI.comment("⚠ Transaksi ${transaksi_id} - Status kosong - Skip")
        continue
    }
    
    if (expectedStatus != 'passed' && expectedStatus != 'failed') {
        WebUI.comment("⚠ Transaksi ${transaksi_id} - Status invalid (harus 'passed' atau 'failed') - Skip")
        continue
    }
    
    WebUI.comment("Transaksi ID: ${transaksi_id}")
    WebUI.comment("Expected Status: ${expectedStatus.toUpperCase()}")
    
    // Count expected
    if (expectedStatus == 'passed') {
        expectedPassed++
    } else {
        expectedFailed++
    }
    
    //(transaksi)
    WebUI.click(findTestObject('Object Repository/m_bsb_lain/Page_Bank Sumsel Babel - Internet Banking/a_Ke Rekening Bank Sumsel Babel Lain'))
    
    WebUI.click(findTestObject('Object Repository/m_bsb_lain/Page_Bank Sumsel Babel - Internet Banking/input__view_transfer_inhouse_new_transfer_inhouse'))
    
    WebUI.waitForPageLoad(30)
    
    // REKENING SUMBER
    WebUI.executeJavaScript("""
    var s = document.querySelector('select[name="accountFromNumber"]');
    var options = s.options;
    for (var i = 0; i < options.length; i++) {
        if (options[i].text == '${rek_sumber}') {
            s.value = options[i].value;
            s.dispatchEvent(new Event('change', { bubbles: true }));
            break;
        }
    }
    """, null)
    
    WebUI.delay(2)
    
    // REKENING TUJUAN
    WebUI.executeJavaScript("""
    var s = document.querySelector('select[name="accountToId"]');
    var options = s.options;
    for (var i = 0; i < options.length; i++) {
        if (options[i].text == '${rek_tujuan}') {
            s.value = options[i].value;
            s.dispatchEvent(new Event('change', { bubbles: true }));
            break;
        }
    }
    """, null)
    
    WebUI.delay(2)
    
    WebUI.setText(findTestObject('Object Repository/m_bsb_lain/Page_Bank Sumsel Babel - Internet Banking/input_Sejumlah_amount'), sejumlah)
    
    WebUI.setText(findTestObject('Object Repository/m_bsb_lain/Page_Bank Sumsel Babel - Internet Banking/input_Catatan_reference'), catatan)
    
    WebUI.click(findTestObject('Object Repository/m_bsb_lain/Page_Bank Sumsel Babel - Internet Banking/input_Pemindahbukuan Segera_new_transfer_da_68e4cb'))
    
    WebUI.click(findTestObject('Object Repository/m_bsb_lain/Page_Bank Sumsel Babel - Internet Banking/fieldset_Detail Jenis Transaksi  Pemindahbu_f15b50'))
    
    WebUI.click(findTestObject('Object Repository/m_bsb_lain/Page_Bank Sumsel Babel - Internet Banking/fieldset_Pemindahbukuan Terjadwal Berulang _079935'))
    
    WebUI.click(findTestObject('Object Repository/m_bsb_lain/Page_Bank Sumsel Babel - Internet Banking/input_Tanggal akhir transaksi_new_transfer__3d9cdd'))
    
    WebUI.delay(2)
    
    // Detect Actual Status dari Web
    String actualStatus = "unknown"
    
    // Cek apakah ada error sebelum M-PIN
    boolean hasErrorBeforeMPIN = WebUI.executeJavaScript("""
        var errorElement = document.querySelector('span.text_error');
        if (errorElement && errorElement.textContent.trim() != '') {
            return true;
        }
        
        var panelResult = document.getElementById('panel_result');
        if (panelResult && panelResult.style.display != 'none') {
            var failedText = panelResult.querySelector('.text_failed');
            if (failedText) {
                return true;
            }
        }
        
        return false;
    """, null)
    
    if (hasErrorBeforeMPIN) {
        actualStatus = "failed"
        WebUI.comment("→ Actual Status: FAILED (error before M-PIN)")
    } else {
        // Cek apakah halaman M-PIN muncul
        boolean hasMPINPage = WebUI.verifyElementPresent(
            findTestObject('Object Repository/m_bsb_lain/Page_Bank Sumsel Babel - Internet Banking/input_M-PIN_otpToken'),
            5,
            FailureHandling.OPTIONAL
        )
        
        if (hasMPINPage) {
            // Lanjut input M-PIN
            WebUI.setText(findTestObject('Object Repository/m_bsb_lain/Page_Bank Sumsel Babel - Internet Banking/input_M-PIN_otpToken'), '111111')
            
            WebUI.setEncryptedText(findTestObject('Object Repository/m_bsb_lain/Page_Bank Sumsel Babel - Internet Banking/input_M-PIN_otpPin'), 'SFTQUhjBfIY=')
            
            WebUI.click(findTestObject('Object Repository/m_bsb_lain/Page_Bank Sumsel Babel - Internet Banking/input_Untuk meminta M-PIN yang baru, silaka_f72c0a'))
            
            WebUI.delay(3)
            
            // Cek hasil akhir
            boolean finalSuccess = WebUI.verifyTextPresent('berhasil', false, FailureHandling.OPTIONAL) ||
                                   WebUI.verifyTextPresent('sukses', false, FailureHandling.OPTIONAL) ||
                                   WebUI.verifyTextPresent('telah dibuat', false, FailureHandling.OPTIONAL)
            
            boolean finalFailed = WebUI.verifyTextPresent('gagal', false, FailureHandling.OPTIONAL) ||
                                  WebUI.verifyTextPresent('Transaksi gagal dilakukan', false, FailureHandling.OPTIONAL)
            
            if (finalSuccess && !finalFailed) {
                actualStatus = "passed"
                WebUI.comment("→ Actual Status: PASSED (transaction successful)")
            } else if (finalFailed) {
                actualStatus = "failed"
                WebUI.comment("→ Actual Status: FAILED (transaction failed after M-PIN)")
            } else {
                actualStatus = "unknown"
                WebUI.comment("→ Actual Status: UNKNOWN (unclear final state)")
                WebUI.takeScreenshot()
            }
        } else {
            actualStatus = "failed"
            WebUI.comment("→ Actual Status: FAILED (M-PIN page not loaded)")
        }
    }
    
    // Count actual
    if (actualStatus == 'passed') {
        actualPassed++
    } else if (actualStatus == 'failed') {
        actualFailed++
    }
    
    totalProcessed++
    
    // Compare Expected vs Actual
    boolean isMatch = (expectedStatus == actualStatus)
    
    if (isMatch) {
        matchCount++
        WebUI.comment("✓ MATCH: Expected ${expectedStatus.toUpperCase()} = Actual ${actualStatus.toUpperCase()}")
    } else {
        mismatchCount++
        WebUI.comment("✗ MISMATCH DETECTED!")
        WebUI.takeScreenshot()
        
        // POP-UP INTERACTIVE untuk Mismatch
        int choice = JOptionPane.showOptionDialog(
            null,
            "⚠️ MISMATCH DETECTED ⚠️\n\n" +
            "Transaksi ID: ${transaksi_id}\n" +
            "Rekening Sumber: ${rek_sumber}\n" +
            "Rekening Tujuan: ${rek_tujuan}\n" +
            "Sejumlah: ${sejumlah}\n" +
            "Catatan: ${catatan}\n\n" +
            "Expected Status: ${expectedStatus.toUpperCase()}\n" +
            "Actual Status: ${actualStatus.toUpperCase()}\n\n" +
            "Screenshot saved for investigation.\n\n" +
            "What do you want to do?",
            "Mismatch Alert - ${transaksi_id}",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE,
            null,
            ["Continue", "Stop Execution", "Mark & Continue"] as Object[],
            "Continue"
        )
        
        if (choice == 1) {
            // Stop Execution
            WebUI.comment("→ User chose to STOP execution")
            userStoppedExecution = true
            break
        } else if (choice == 2) {
            // Mark & Continue (sama seperti Continue tapi dengan marking khusus)
            WebUI.comment("→ User marked mismatch and will continue")
        } else {
            // Continue (default)
            WebUI.comment("→ User chose to CONTINUE")
        }
    }
    
    WebUI.delay(2)
    WebUI.comment("✓ Transaksi ${transaksi_id} selesai diproses")
    //(/transaksi)
    
    // Break jika user stop
    if (userStoppedExecution) {
        break
    }
}

//batas looping dan summary
WebUI.comment("")
WebUI.comment("==========================================")
WebUI.comment("=== SUMMARY REPORT ===")
WebUI.comment("==========================================")
WebUI.comment("Total transaksi diproses: ${totalProcessed}")
WebUI.comment("")
WebUI.comment("--- Expected Status ---")
WebUI.comment("Expected PASSED: ${expectedPassed}")
WebUI.comment("Expected FAILED: ${expectedFailed}")
WebUI.comment("")
WebUI.comment("--- Actual Status ---")
WebUI.comment("Actual PASSED: ${actualPassed}")
WebUI.comment("Actual FAILED: ${actualFailed}")
WebUI.comment("")
WebUI.comment("--- Validation Result ---")
WebUI.comment("MATCH: ${matchCount} (${totalProcessed > 0 ? String.format('%.1f', matchCount * 100.0 / totalProcessed) : '0'}%)")
WebUI.comment("MISMATCH: ${mismatchCount} (${totalProcessed > 0 ? String.format('%.1f', mismatchCount * 100.0 / totalProcessed) : '0'}%)")
WebUI.comment("")

// Final Assessment
if (userStoppedExecution) {
    WebUI.comment("⚠ Execution stopped by user at transaksi ${totalProcessed}")
} else if (mismatchCount == 0 && totalProcessed > 0) {
    WebUI.comment("✓✓✓ ALL TESTS PASSED - 100% MATCH ✓✓✓")
} else if (mismatchCount > 0) {
    WebUI.comment("⚠⚠⚠ ${mismatchCount} MISMATCH FOUND - Review screenshots ⚠⚠⚠")
}

WebUI.comment("==========================================")

//selesai
WebUI.comment("=== Semua transaksi selesai ===")
WebUI.click(findTestObject('Object Repository/m_bsb_lain/Page_Bank Sumsel Babel - Internet Banking/a_Keluar'))
WebUI.delay(2)
WebUI.closeBrowser()
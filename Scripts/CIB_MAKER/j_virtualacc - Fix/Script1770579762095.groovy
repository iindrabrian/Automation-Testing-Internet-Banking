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
// SCRIPT PEMBAYARAN VIRTUAL ACCOUNT
// ========================================

// Load semua data dari databasex
def allData = findTestData('Data Files/data_bsb_lain')

// Hitung jumlah transaksi yang valid
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

// Loop berdasarkan jumlah transaksi yang valid
for (int i = 1; i <= totalRows; i++) {
    WebUI.comment("=== Processing transaksi ${i}/${totalRows} ===")

    def transaksi_id = allData.getValue('transaksi_id', i)
    def rek_sumber = allData.getValue('BL_rek_sumber', i)
    def nomor_va = allData.getValue('nomor_va', i)

    // Validasi data wajib
    if (rek_sumber == null || rek_sumber.toString().trim().isEmpty() ||
        nomor_va == null || nomor_va.toString().trim().isEmpty()) {
        WebUI.comment("⚠ Transaksi ${transaksi_id} invalid - Skip ke transaksi berikutnya")
        continue
    }

    WebUI.comment("Transaksi ID: ${transaksi_id}")

    //(transaksi)
    WebUI.click(findTestObject('Object Repository/m_virtualacc/Page_Bank Sumsel Babel - Internet Banking/a_Pembayaran Virtual Account'))

    WebUI.click(findTestObject('Object Repository/m_virtualacc/Page_Bank Sumsel Babel - Internet Banking/input_halaman1dari0_payment_va_view_payment_5de0fe'))

    WebUI.waitForPageLoad(30)

    // REKENING SUMBER
    WebUI.executeJavaScript("""
    var s = document.querySelector('select[name="accountSource.number"]');
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
    
    // PILIH RADIO BUTTON "Nomor Virtual Account"
    WebUI.click(findTestObject('Object Repository/m_virtualacc/Page_Bank Sumsel Babel - Internet Banking/input_Nomor Virtual Account_inputVANEW'))
    
    WebUI.delay(1)
    
    // INPUT NOMOR VA
    WebUI.setText(findTestObject('Object Repository/m_virtualacc/Page_Bank Sumsel Babel - Internet Banking/input_Nomor Virtual Account_vaNumberNew'), nomor_va.toString())
    
    WebUI.delay(2)
    
	// Simpan Nomor VA
	WebUI.click(findTestObject('Object Repository/m_virtualacc/Page_Bank Sumsel Babel - Internet Banking/input_Silakan masukkan nomor virtual accoun_455b90'))
	
	WebUI.delay(2)
	
	// Submit - Kirim
	WebUI.click(findTestObject('Object Repository/m_virtualacc/Page_Bank Sumsel Babel - Internet Banking/input_Nomor Virtual Account_submitPaymentInput'))
	
	WebUI.delay(2)
	
	// Cek error - UPDATE dengan selector yang benar
	boolean hasError = WebUI.executeJavaScript("""
	    // Cek div panel_result
	    var panelResult = document.getElementById('panel_result');
	    if (panelResult && panelResult.style.display != 'none') {
	        var failedText = panelResult.querySelector('.text_failed');
	        if (failedText && failedText.textContent.includes('Transaksi gagal dilakukan')) {
	            return true;
	        }
	    }
	    
	    // Cek span.text_error juga (fallback)
	    var errorElement = document.querySelector('span.text_error');
	    if (errorElement && errorElement.textContent.trim() != '') {
	        return true;
	    }
	    
	    return false;
	""", null)
	
	if (hasError) {
	    WebUI.comment("⚠ Transaksi gagal dilakukan - Skip ke transaksi berikutnya")
	    WebUI.takeScreenshot() // Dokumentasi error
	    continue
	}
	
    // Input M-PIN (di halaman konfirmasi)
    WebUI.setText(findTestObject('Object Repository/m_tagihan/Page_Bank Sumsel Babel - Internet Banking/input_M-PIN_otpToken'), '111111')
    
    WebUI.setEncryptedText(findTestObject('Object Repository/m_tagihan/Page_Bank Sumsel Babel - Internet Banking/input_M-PIN_otpPin'), 'SFTQUhjBfIY=')

    WebUI.click(findTestObject('Object Repository/m_bsb_lain/Page_Bank Sumsel Babel - Internet Banking/input_Untuk meminta M-PIN yang baru, silaka_f72c0a'))


    WebUI.delay(2)
    WebUI.comment("✓ Transaksi ${transaksi_id} selesai")
    //(/transaksi)
}

//batas looping dan selesai
WebUI.comment("=== Semua transaksi selesai ===")
WebUI.click(findTestObject('Object Repository/m_approval/Page_Bank Sumsel Babel - Internet Banking/a_Keluar'))
WebUI.delay(2)
WebUI.closeBrowser()
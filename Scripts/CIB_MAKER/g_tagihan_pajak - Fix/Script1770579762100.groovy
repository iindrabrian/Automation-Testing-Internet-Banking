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
// SCRIPT 4: PAJAK (4 sub-jenis)
// ========================================

// Load semua data dari database
def allData = findTestData('Data Files/data_bsb_lain_tagihan')

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
    def jenis_pembayaran4 = allData.getValue('jenis_pembayaran4', i)
    def jenis_transaksi4 = allData.getValue('jenis_transaksi4', i)
    def kode_pajak = allData.getValue('kode_pajak', i)
    def kode_area = allData.getValue('kode_area', i)
    def npwp = allData.getValue('npwp', i)
    def periode = allData.getValue('periode', i)
    def cicilan = allData.getValue('cicilan', i)
    def prosedur = allData.getValue('prosedur', i)
    def id_billing = allData.getValue('id_billing', i)

    // Validasi data wajib minimal
    if (rek_sumber == null || rek_sumber.toString().trim().isEmpty() ||
        jenis_pembayaran4 == null || jenis_pembayaran4.toString().trim().isEmpty() ||
        jenis_transaksi4 == null || jenis_transaksi4.toString().trim().isEmpty()) {
        WebUI.comment("⚠ Transaksi ${transaksi_id} invalid - Skip ke transaksi berikutnya")
        continue
    }

    WebUI.comment("Transaksi ID: ${transaksi_id}")

    //(transaksi)
    WebUI.click(findTestObject('Object Repository/m_tagihan/Page_Bank Sumsel Babel - Internet Banking/a_Pembayaran Tagihan'))

    WebUI.click(findTestObject('Object Repository/m_tagihan/Page_Bank Sumsel Babel - Internet Banking/input_halaman1dari1_payment_view_payment_input'))

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
    
    // JENIS PEMBAYARAN
    WebUI.executeJavaScript("""
    var s = document.getElementById('providerId');
    var options = s.options;
    for (var i = 0; i < options.length; i++) {
        if (options[i].text == '${jenis_pembayaran4}') {
            s.value = options[i].value;
            s.dispatchEvent(new Event('change', { bubbles: true }));
            break;
        }
    }
    """, null)
    
    WebUI.delay(2)
    
    // JENIS TRANSAKSI
    WebUI.executeJavaScript("""
    var s = document.getElementById('subProviderId');
    var options = s.options;
    for (var i = 0; i < options.length; i++) {
        if (options[i].text == '${jenis_transaksi4}') {
            s.value = options[i].value;
            s.dispatchEvent(new Event('change', { bubbles: true }));
            break;
        }
    }
    """, null)
    
    WebUI.delay(2)
    
    // Handle berdasarkan jenis transaksi
    if (jenis_transaksi4 == 'Pajak Daerah') {
        // Validasi field wajib
        if (kode_pajak == null || kode_area == null || npwp == null || periode == null || cicilan == null) {
            WebUI.comment("⚠ Field tidak lengkap untuk Pajak Daerah - Skip ${transaksi_id}")
            continue
        }
        
        // KODE PAJAK
        WebUI.executeJavaScript("""
        var s = document.getElementById('taxCode');
        var options = s.options;
        for (var i = 0; i < options.length; i++) {
            if (options[i].text.startsWith('${kode_pajak}')) {
                s.value = options[i].value;
                s.dispatchEvent(new Event('change', { bubbles: true }));
                break;
            }
        }
        """, null)
        
        WebUI.delay(1)
        
        // KODE AREA
        WebUI.executeJavaScript("""
        var s = document.getElementById('areaCode');
        var options = s.options;
        for (var i = 0; i < options.length; i++) {
            if (options[i].text.startsWith('${kode_area}')) {
                s.value = options[i].value;
                s.dispatchEvent(new Event('change', { bubbles: true }));
                break;
            }
        }
        """, null)
        
        WebUI.delay(1)
        
        // NPWP
        WebUI.setText(findTestObject('Object Repository/m_tagihan/Page_Bank Sumsel Babel - Internet Banking/input_NPWP_npwp'), npwp.toString())
        
        // PERIODE
        WebUI.setText(findTestObject('Object Repository/m_tagihan/Page_Bank Sumsel Babel - Internet Banking/input_Periode  Kode Bayar_period'), periode.toString())
        
        // CICILAN
        WebUI.setText(findTestObject('Object Repository/m_tagihan/Page_Bank Sumsel Babel - Internet Banking/input_Cicilan_installment'), cicilan.toString())
        
    } else if (jenis_transaksi4 == 'Pajak Daerah Kode Bayar') {
        // Validasi field wajib
        if (kode_area == null || periode == null) {
            WebUI.comment("⚠ Field tidak lengkap untuk Pajak Daerah Kode Bayar - Skip ${transaksi_id}")
            continue
        }
        
        // KODE AREA
        WebUI.executeJavaScript("""
        var s = document.getElementById('areaCode');
        var options = s.options;
        for (var i = 0; i < options.length; i++) {
            if (options[i].text.startsWith('${kode_area}')) {
                s.value = options[i].value;
                s.dispatchEvent(new Event('change', { bubbles: true }));
                break;
            }
        }
        """, null)
        
        WebUI.delay(1)
        
        // PERIODE
        WebUI.setText(findTestObject('Object Repository/m_tagihan/Page_Bank Sumsel Babel - Internet Banking/input_Periode  Kode Bayar_period'), periode.toString())
        
    } else if (jenis_transaksi4 == 'BPHTB') {
        // Validasi field wajib
        if (npwp == null) {
            WebUI.comment("⚠ NPWP tidak ada untuk BPHTB - Skip ${transaksi_id}")
            continue
        }
        
        // NOMOR PAJAK/NPWP
        WebUI.setText(findTestObject('Object Repository/m_tagihan/Page_Bank Sumsel Babel - Internet Banking/input_NPWP_npwp'), npwp.toString())
        
    } else if (jenis_transaksi4 == 'MPN G2') {
        // Validasi field wajib
        if (prosedur == null || id_billing == null) {
            WebUI.comment("⚠ Field tidak lengkap untuk MPN G2 - Skip ${transaksi_id}")
            continue
        }
        
        // PROSEDUR
        WebUI.executeJavaScript("""
        var s = document.getElementById('operationId');
        var options = s.options;
        for (var i = 0; i < options.length; i++) {
            if (options[i].text.includes('${prosedur}')) {
                s.value = options[i].value;
                s.dispatchEvent(new Event('change', { bubbles: true }));
                break;
            }
        }
        """, null)
        
        WebUI.delay(1)
        
        // NOMOR BILLING
        WebUI.setText(findTestObject('Object Repository/m_tagihan/Page_Bank Sumsel Babel - Internet Banking/input_Nomor Billing_custNoId'), id_billing.toString())
    }
    
    WebUI.delay(2)
    
	// Submit
	WebUI.click(findTestObject('Object Repository/m_tagihan/Page_Bank Sumsel Babel - Internet Banking/input_Silahkan masukan nomor pelanggan anda_811a1b'))
	
	WebUI.delay(2)
	
	// Cek error
	boolean hasError = WebUI.executeJavaScript("""
	    var errorElement = document.querySelector('span.text_error');
	    if (errorElement && (errorElement.textContent.trim() != '' || 
	                         errorElement.textContent.includes('Transaksi anda gagal dilakukan'))) {
	        return true;
	    }
	    return false;
	""", null)
	
	if (hasError) {
	    WebUI.comment("⚠ Error validasi atau transaksi gagal - Skip ke transaksi berikutnya")
	    continue
	}
    
    WebUI.setText(findTestObject('Object Repository/m_bsb_lain/Page_Bank Sumsel Babel - Internet Banking/input_M-PIN_otpToken'), '111111')
    
    WebUI.setEncryptedText(findTestObject('Object Repository/m_bsb_lain/Page_Bank Sumsel Babel - Internet Banking/input_M-PIN_otpPin'), 'SFTQUhjBfIY=')
    
    WebUI.click(findTestObject('Object Repository/m_bsb_lain/Page_Bank Sumsel Babel - Internet Banking/input_Untuk meminta M-PIN yang baru, silaka_f72c0a'))
		
    WebUI.delay (2)
    WebUI.comment("✓ Transaksi ${transaksi_id} selesai")
    //(/transaksi)
}

//batas looping dan selesai
WebUI.comment("=== Semua transaksi selesai ===")
WebUI.click(findTestObject('Object Repository/m_tagihan/Page_Bank Sumsel Babel - Internet Banking/a_Keluar'))
WebUI.delay(2)
WebUI.closeBrowser()
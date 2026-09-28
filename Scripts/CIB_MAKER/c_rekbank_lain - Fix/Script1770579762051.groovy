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

// Loop berdasarkan jumlah transaksi yang valid
for (int i = 1; i <= totalRows; i++) {
    WebUI.comment("=== Processing transaksi ${i}/${totalRows} ===")

    def transaksi_id = allData.getValue('transaksi_id', i)
    def rek_sumber = allData.getValue('BL_rek_sumber', i)
    def rek_tujuan = allData.getValue('rek_tujuan', i)
    def sejumlah = allData.getValue('sejumlah', i)
    def catatan = allData.getValue('catatan', i)
    def layanan = allData.getValue('layanan', i)
    def Informasi = allData.getValue('informasi', i)

    // Validasi data wajib - jika tidak valid, skip ke transaksi berikutnya
    if (rek_sumber == null || rek_sumber.toString().trim().isEmpty() ||
        rek_tujuan == null || rek_tujuan.toString().trim().isEmpty() ||
        sejumlah == null || sejumlah.toString().trim().isEmpty()) {
        WebUI.comment("⚠ Transaksi ${transaksi_id} invalid - Skip ke transaksi berikutnya")
        continue
    }

    WebUI.comment("Transaksi ID: ${transaksi_id}")

    //(transaksi)
    WebUI.click(findTestObject('Object Repository/m_rekbank_lain/Page_Bank Sumsel Babel - Internet Banking/a_Ke Rekening Bank Lain'))

    WebUI.click(findTestObject('Object Repository/m_rekbank_lain/Page_Bank Sumsel Babel - Internet Banking/input_Tidak ada data untuk ditampilkan_view_75708f'))

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
    
    // RADIO BUTTON LAYANAN
    def layananMap = [
        'SKN': '0',
        'RTGS': '1',
        'Realtime': '2',
        'Bifast': '3'
    ]
    
    String radioValue = layananMap[layanan]
    assert radioValue != null : "Layanan ${layanan} tidak ditemukan!"
    
    WebUI.executeJavaScript("""
    var radios = document.querySelectorAll('input[name="sknrtgs"]');
    for (var i = 0; i < radios.length; i++) {
        if (radios[i].value == '${radioValue}') {
            radios[i].checked = true;
            radios[i].dispatchEvent(new Event('change', { bubbles: true }));
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

    WebUI.setText(findTestObject('Object Repository/m_rekbank_lain/Page_Bank Sumsel Babel - Internet Banking/input_Sejumlah_amountId'), sejumlah)

    WebUI.setText(findTestObject('Object Repository/m_rekbank_lain/Page_Bank Sumsel Babel - Internet Banking/input_Catatan_notesId'), catatan)
    
	// INFORMASI TRANSAKSI - berdasarkan layanan
	if (layanan == 'SKN') {
	    // Validasi format
	    def infoParts = Informasi.split(';')
	    if (infoParts.length < 3) {
	        WebUI.comment("⚠ Format Informasi SKN tidak valid (expected: WNI;Resident;Perorangan) - Skip transaksi ${transaksi_id}")
	        continue
	    }
	    
	    String citizenship = infoParts[0].trim()
	    String residential = infoParts[1].trim()
	    String nasabahPenerima = infoParts[2].trim()
	    
	    // Info kewarganegaraan
	    def citizenshipMap = ['WNI': '0', 'Non-WNI': '1']
	    String citizenshipValue = citizenshipMap[citizenship]
	    
	    WebUI.executeJavaScript("""
	    var radios = document.querySelectorAll('input[name="citizenship"]');
	    for (var i = 0; i < radios.length; i++) {
	        if (radios[i].value == '${citizenshipValue}') {
	            radios[i].checked = true;
	            radios[i].dispatchEvent(new Event('change', { bubbles: true }));
	            break;
	        }
	    }
	    """, null)
	    
	    // Resident Info
	    def residentialMap = ['Resident': '0', 'Non-Resident': '1']
	    String residentialValue = residentialMap[residential]
	    
	    WebUI.executeJavaScript("""
	    var radios = document.querySelectorAll('input[name="residential"]');
	    for (var i = 0; i < radios.length; i++) {
	        if (radios[i].value == '${residentialValue}') {
	            radios[i].checked = true;
	            radios[i].dispatchEvent(new Event('change', { bubbles: true }));
	            break;
	        }
	    }
	    """, null)
	    
	    // Nasabah Penerima
	    def ltypeMap = ['Perorangan': '0', 'Perusahaan': '1', 'Pemerintah': '2', 'Remittance': '3']
	    String ltypeValue = ltypeMap[nasabahPenerima]
	    
	    WebUI.executeJavaScript("""
	    var radios = document.querySelectorAll('input[name="ltype"]');
	    for (var i = 0; i < radios.length; i++) {
	        if (radios[i].value == '${ltypeValue}') {
	            radios[i].checked = true;
	            radios[i].dispatchEvent(new Event('change', { bubbles: true }));
	            break;
	        }
	    }
	    """, null)
	    
	} else if (layanan == 'RTGS') {
	    // Validasi format: "WNI;Resident;Kelapa dua - Tangerang"
	    def infoParts = Informasi.split(';')
	    if (infoParts.length < 2) {
	        WebUI.comment("⚠ Format Informasi RTGS tidak valid (expected: WNI;Resident;Alamat) - Skip transaksi ${transaksi_id}")
	        continue
	    }
	    
	    String citizenship = infoParts[0].trim()
	    String residential = infoParts[1].trim()
	    String alamatPenerima = infoParts.length > 2 ? infoParts[2].trim() : ""
	    
	    // Info kewarganegaraan
	    def citizenshipMap = ['WNI': '0', 'Non-WNI': '1']
	    String citizenshipValue = citizenshipMap[citizenship]
	    
	    WebUI.executeJavaScript("""
	    var radios = document.querySelectorAll('input[name="citizenship"]');
	    for (var i = 0; i < radios.length; i++) {
	        if (radios[i].value == '${citizenshipValue}') {
	            radios[i].checked = true;
	            radios[i].dispatchEvent(new Event('change', { bubbles: true }));
	            break;
	        }
	    }
	    """, null)
	    
	    // Resident Info
	    def residentialMap = ['Resident': '0', 'Non-Resident': '1']
	    String residentialValue = residentialMap[residential]
	    
	    WebUI.executeJavaScript("""
	    var radios = document.querySelectorAll('input[name="residential"]');
	    for (var i = 0; i < radios.length; i++) {
	        if (radios[i].value == '${residentialValue}') {
	            radios[i].checked = true;
	            radios[i].dispatchEvent(new Event('change', { bubbles: true }));
	            break;
	        }
	    }
	    """, null)
	    
	    // Alamat Penerima - isi ke field pertama
	    if (!alamatPenerima.isEmpty()) {
	        WebUI.setText(findTestObject('Object Repository/m_rekbank_lain/Page_Bank Sumsel Babel - Internet Banking/input_Alamat Penerima_beneficiaryAddress1'), alamatPenerima)
	    }
	    
	} else if (layanan == 'Realtime') {
	    // Validasi format
	    def infoParts = Informasi.split(';')
	    if (infoParts.length < 2) {
	        WebUI.comment("⚠ Format Informasi Realtime tidak valid (expected: WNI;Resident) - Skip transaksi ${transaksi_id}")
	        continue
	    }
	    
	    String citizenship = infoParts[0].trim()
	    String residential = infoParts[1].trim()
	    
	    // Info kewarganegaraan
	    def citizenshipMap = ['WNI': '0', 'Non-WNI': '1']
	    String citizenshipValue = citizenshipMap[citizenship]
	    
	    WebUI.executeJavaScript("""
	    var radios = document.querySelectorAll('input[name="citizenship"]');
	    for (var i = 0; i < radios.length; i++) {
	        if (radios[i].value == '${citizenshipValue}') {
	            radios[i].checked = true;
	            radios[i].dispatchEvent(new Event('change', { bubbles: true }));
	            break;
	        }
	    }
	    """, null)
	    
	    // Resident Info
	    def residentialMap = ['Resident': '0', 'Non-Resident': '1']
	    String residentialValue = residentialMap[residential]
	    
	    WebUI.executeJavaScript("""
	    var radios = document.querySelectorAll('input[name="residential"]');
	    for (var i = 0; i < radios.length; i++) {
	        if (radios[i].value == '${residentialValue}') {
	            radios[i].checked = true;
	            radios[i].dispatchEvent(new Event('change', { bubbles: true }));
	            break;
	        }
	    }
	    """, null)
	    
	} else if (layanan == 'Bifast') {
	    // Validasi format
	    if (Informasi == null || Informasi.trim().isEmpty()) {
	        WebUI.comment("⚠ Informasi Bifast kosong - Skip transaksi ${transaksi_id}")
	        continue
	    }
	    
	    String tujuanTransaksi = Informasi.trim()
	    
	    // Tujuan Transaksi
	    def tujuanMap = ['Investasi': '1', 'Transfer Dana': '2', 'Pembelian': '3', 'Lainnya': '4']
	    String tujuanValue = tujuanMap[tujuanTransaksi]
	    
	    WebUI.executeJavaScript("""
	    var s = document.getElementById('transferPurpose');
	    s.value = '${tujuanValue}';
	    s.dispatchEvent(new Event('change', { bubbles: true }));
	    """, null)
	    
	    WebUI.delay(2)
	    
	    // Checkbox Syarat dan Ketentuan (khusus Bifast)
	    WebUI.click(findTestObject('m_rekbank_lain/Page_Bank Sumsel Babel - Internet Banking/input_Syarat dan Ketentuan_transfer_third_agree'))
	}
    
    WebUI.delay(2)
    
    // Konfirmasi transfer
    WebUI.click(findTestObject('Object Repository/m_rekbank_lain/Page_Bank Sumsel Babel - Internet Banking/input_Tanggal akhir transaksi_transfer_thir_29bede'))

    WebUI.setText(findTestObject('Object Repository/m_bsb_lain/Page_Bank Sumsel Babel - Internet Banking/input_M-PIN_otpToken'), '111111')

    WebUI.setEncryptedText(findTestObject('Object Repository/m_bsb_lain/Page_Bank Sumsel Babel - Internet Banking/input_M-PIN_otpPin'), 'SFTQUhjBfIY=')

    WebUI.click(findTestObject('Object Repository/m_bsb_lain/Page_Bank Sumsel Babel - Internet Banking/input_Untuk meminta M-PIN yang baru, silaka_f72c0a'))

    WebUI.delay(2)
    WebUI.comment("✓ Transaksi ${transaksi_id} selesai")
    //(/transaksi)
}

//batas looping dan selesai
WebUI.comment("=== Semua transaksi selesai ===")
WebUI.click(findTestObject('Object Repository/m_bsb_lain/Page_Bank Sumsel Babel - Internet Banking/a_Keluar'))
WebUI.delay(2)
WebUI.closeBrowser()
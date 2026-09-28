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
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.text.SimpleDateFormat

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
// SCRIPT TRANSFER DANA BOS
// Dengan ISO 8583 Response Code Validation
// ========================================

import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.text.SimpleDateFormat

// Load semua data dari database
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

// Statistics tracking
int totalProcessed = 0
int matchCount = 0
int mismatchCount = 0

// ISO 8583 Response Code Mapping
Map<String, String> responseCodeMap = [
    '00': 'Approved',
    '13': 'Invalid Amount',
    '14': 'Invalid Card Number',
    '30': 'Format Error - Invalid Kodifikasi',
    '51': 'Insufficient Funds',
    '54': 'Expired Card',
    '55': 'Incorrect PIN',
    '61': 'Exceeds Withdrawal Limit',
    '91': 'Issuer Not Available',
    '99': 'Transaction Failed - Unknown Error'
]

// Prepare Excel Workbook untuk Mismatch Report
def workbook = new XSSFWorkbook()
def sheet = workbook.createSheet("Mismatch Report")

// Create header row dengan styling
def headerRow = sheet.createRow(0)
def headerStyle = workbook.createCellStyle()
def headerFont = workbook.createFont()
headerFont.setBold(true)
headerFont.setFontHeightInPoints((short) 12)
headerStyle.setFont(headerFont)
headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex())
headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND)

def headers = ["No", "Timestamp", "Transaksi ID", "Rek Sumber (Excel)", "Found?", "Rek Tujuan (Excel)", "Found?", "Sejumlah", "Kodifikasi", "Kodifikasi Valid?", "Expected Code", "Expected Message", "Actual Code", "Actual Message", "Issue Detected", "Screenshot Path"]
headers.eachWithIndex { header, idx ->
    def cell = headerRow.createCell(idx)
    cell.setCellValue(header)
    cell.setCellStyle(headerStyle)
}

int mismatchRowNum = 1

// Loop berdasarkan jumlah transaksi yang valid
for (int i = 1; i <= totalRows; i++) {
    WebUI.comment("=== Processing transaksi ${i}/${totalRows} ===")

    def transaksi_id = allData.getValue('transaksi_id', i)
    def rek_sumber = allData.getValue('rek_sumber', i)
    def rek_tujuan = allData.getValue('rek_tujuan', i)
    def sejumlah = allData.getValue('sejumlah', i)
    def kodifikasi = allData.getValue('kodifikasi', i)

    // Validasi data wajib
    if (rek_sumber == null || rek_sumber.toString().trim().isEmpty() ||
        rek_tujuan == null || rek_tujuan.toString().trim().isEmpty() ||
        sejumlah == null || sejumlah.toString().trim().isEmpty()) {
        WebUI.comment("⚠ Transaksi ${transaksi_id} invalid (data tidak lengkap) - Skip")
        continue
    }

    WebUI.comment("Transaksi ID: ${transaksi_id}")

    //(transaksi)
    WebUI.click(findTestObject('Object Repository/m_dana_bos/Page_Bank Sumsel Babel - Internet Banking/a_Rekening Dana BOS'))

    WebUI.click(findTestObject('Object Repository/m_dana_bos/Page_Bank Sumsel Babel - Internet Banking/input__view_transfer_bos_new_transfer_inhouse'))

    WebUI.waitForPageLoad(30)

    // ===== PRE-VALIDATION: CEK APAKAH DATA ADA DI DROPDOWN =====
    
    // Cek Rekening Sumber
    def rekSumberCheck = WebUI.executeJavaScript("""
        var s = document.querySelector('select[name="accountFromNumber"]');
        var options = s.options;
        var found = false;
        var selectedValue = null;
        
        for (var i = 0; i < options.length; i++) {
            if (options[i].text.trim() == '${rek_sumber}') {
                found = true;
                selectedValue = options[i].value;
                s.value = selectedValue;
                s.dispatchEvent(new Event('change', { bubbles: true }));
                break;
            }
        }
        
        return {found: found, value: selectedValue};
    """, null)
    
    boolean rekSumberFound = rekSumberCheck.found
    WebUI.comment("→ Rek Sumber '${rek_sumber}': ${rekSumberFound ? 'FOUND ✓' : 'NOT FOUND ✗'}")

    WebUI.delay(2)

    // Cek Rekening Tujuan
    def rekTujuanCheck = WebUI.executeJavaScript("""
        var s = document.querySelector('select[name="accountToId"]');
        var options = s.options;
        var found = false;
        var selectedValue = null;
        
        for (var i = 0; i < options.length; i++) {
            if (options[i].text.trim() == '${rek_tujuan}') {
                found = true;
                selectedValue = options[i].value;
                s.value = selectedValue;
                s.dispatchEvent(new Event('change', { bubbles: true }));
                break;
            }
        }
        
        return {found: found, value: selectedValue};
    """, null)
    
    boolean rekTujuanFound = rekTujuanCheck.found
    WebUI.comment("→ Rek Tujuan '${rek_tujuan}': ${rekTujuanFound ? 'FOUND ✓' : 'NOT FOUND ✗'}")

    WebUI.delay(2)

    WebUI.setText(findTestObject('Object Repository/m_dana_bos/Page_Bank Sumsel Babel - Internet Banking/input_Sejumlah_amount'), sejumlah)

    // ===== VALIDASI KODIFIKASI =====
    
    def parts = kodifikasi.split('\\.')
    boolean kodifikasiValid = true
    String kodifikasiIssue = ""

    // Level 1: Kategori
    def categoryCheck = WebUI.executeJavaScript("""
        var s = document.getElementById('danaBosCategoryId');
        var options = s.options;
        var searchText = '${parts[0]}.';
        var found = false;
        
        for (var i = 0; i < options.length; i++) {
            if (options[i].text.trim().startsWith(searchText)) {
                s.value = options[i].value;
                s.dispatchEvent(new Event('change', { bubbles: true }));
                found = true;
                break;
            }
        }
        
        return found;
    """, null)
    
    if (!categoryCheck) {
        kodifikasiValid = false
        kodifikasiIssue = "Kategori '${parts[0]}' not found"
    }

    WebUI.delay(2)

    // Level 2: Component
    def componentCheck = WebUI.executeJavaScript("""
        var s = document.getElementById('danaBosComponentId');
        var options = s.options;
        var searchText = '${parts[1]}.';
        var found = false;
        
        for (var i = 0; i < options.length; i++) {
            if (options[i].text.trim().startsWith(searchText)) {
                s.value = options[i].value;
                s.dispatchEvent(new Event('change', { bubbles: true }));
                found = true;
                break;
            }
        }
        
        return found;
    """, null)
    
    if (!componentCheck) {
        kodifikasiValid = false
        kodifikasiIssue = "Component '${parts[1]}' not found"
    }

    WebUI.delay(2)

    // Level 3: Sub Component
    def subComponentCheck = WebUI.executeJavaScript("""
        var s = document.getElementById('danaBosSubComponentId');
        var options = s.options;
        var searchText = '${parts[2]}.';
        var found = false;
        
        for (var i = 0; i < options.length; i++) {
            if (options[i].text.trim().startsWith(searchText)) {
                s.value = options[i].value;
                s.dispatchEvent(new Event('change', { bubbles: true }));
                found = true;
                break;
            }
        }
        
        return found;
    """, null)
    
    if (!subComponentCheck) {
        kodifikasiValid = false
        kodifikasiIssue = "Sub Component '${parts[2]}' not found"
    }

    WebUI.delay(2)

    // Level 4: Shop (jika ada)
    if (parts.length > 3) {
        def shopCheck = WebUI.executeJavaScript("""
            var s = document.getElementById('danaBosShopId');
            var options = s.options;
            var searchText = '${parts[3]}';
            var found = false;
            
            for (var i = 0; i < options.length; i++) {
                if (options[i].text.trim().startsWith(searchText)) {
                    s.value = options[i].value;
                    s.dispatchEvent(new Event('change', { bubbles: true }));
                    found = true;
                    break;
                }
            }
            
            return found;
        """, null)
        
        if (!shopCheck) {
            kodifikasiValid = false
            kodifikasiIssue = "Shop '${parts[3]}' not found"
        }
        
        WebUI.delay(2)
    }
    
    WebUI.comment("→ Kodifikasi '${kodifikasi}': ${kodifikasiValid ? 'VALID ✓' : 'INVALID ✗ (' + kodifikasiIssue + ')'}")

    // ===== DETERMINE EXPECTED RESPONSE CODE =====
    
    String expectedCode = "00"
    String expectedMessage = "Approved"
    String issueDetected = "None"
    
    // Rule 1: Invalid Card Number (Rekening tidak ditemukan)
    if (!rekSumberFound) {
        expectedCode = "14"
        expectedMessage = "Invalid Card Number - Source Account Not Found"
        issueDetected = "Rek Sumber tidak ada di dropdown"
    } else if (!rekTujuanFound) {
        expectedCode = "14"
        expectedMessage = "Invalid Card Number - Destination Account Not Found"
        issueDetected = "Rek Tujuan tidak ada di dropdown"
    }
    
    // Rule 2: Invalid Kodifikasi (Format Error)
    if (!kodifikasiValid) {
        expectedCode = "30"
        expectedMessage = "Format Error - Invalid Kodifikasi"
        issueDetected = kodifikasiIssue
    }
    
    // Rule 3: Invalid Amount
    def amountValue = sejumlah.toString().replaceAll('[^0-9.-]', '')
    if (amountValue.contains('-') || amountValue == '0' || amountValue.toDouble() <= 0) {
        expectedCode = "13"
        expectedMessage = "Invalid Amount - Negative or Zero"
        issueDetected = "Jumlah invalid (minus atau nol)"
    }
    
    // Rule 4: Exceeds Limit
    if (amountValue.toDouble() > 1000000000) {
        expectedCode = "61"
        expectedMessage = "Exceeds Withdrawal Limit"
        issueDetected = "Jumlah melebihi batas maksimal"
    }
    
    WebUI.comment("Expected Response: ${expectedCode} - ${expectedMessage}")
    
    // ===== LANJUTKAN TRANSAKSI (Meski expected FAILED) =====
    
    // Jika dropdown tidak found, pilih random untuk simulasi
    if (!rekSumberFound) {
        WebUI.executeJavaScript("""
            var s = document.querySelector('select[name="accountFromNumber"]');
            if (s.options.length > 1) {
                s.selectedIndex = 1;
                s.dispatchEvent(new Event('change', { bubbles: true }));
            }
        """, null)
        WebUI.delay(2)
    }
    
    if (!rekTujuanFound) {
        WebUI.executeJavaScript("""
            var s = document.querySelector('select[name="accountToId"]');
            if (s.options.length > 1) {
                s.selectedIndex = 1;
                s.dispatchEvent(new Event('change', { bubbles: true }));
            }
        """, null)
        WebUI.delay(2)
    }

    WebUI.click(findTestObject('Object Repository/m_dana_bos/Page_Bank Sumsel Babel - Internet Banking/input_Pemindahbukuan Segera_new_transfer_da_68e4cb'))

    WebUI.click(findTestObject('Object Repository/m_bsb_lain/Page_Bank Sumsel Babel - Internet Banking/fieldset_Detail Jenis Transaksi  Pemindahbu_f15b50'))

    WebUI.click(findTestObject('Object Repository/m_bsb_lain/Page_Bank Sumsel Babel - Internet Banking/fieldset_Pemindahbukuan Terjadwal Berulang _079935'))

    WebUI.click(findTestObject('Object Repository/m_bsb_lain/Page_Bank Sumsel Babel - Internet Banking/input_Tanggal akhir transaksi_new_transfer__3d9cdd'))
    
    WebUI.delay(2)
    
    // ===== DETECT ACTUAL RESPONSE =====
    
    String actualCode = "00"
    String actualMessage = "Approved"
    
    // Cek error sebelum M-PIN
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
        actualCode = "99"
        actualMessage = "Transaction Failed"
        WebUI.comment("→ Actual Response: ${actualCode} - ${actualMessage}")
    } else {
        // Cek M-PIN page
        boolean hasMPINPage = WebUI.verifyElementPresent(
            findTestObject('Object Repository/m_bsb_lain/Page_Bank Sumsel Babel - Internet Banking/input_M-PIN_otpToken'),
            5,
            FailureHandling.OPTIONAL
        )
        
        if (hasMPINPage) {
            WebUI.setText(findTestObject('Object Repository/m_bsb_lain/Page_Bank Sumsel Babel - Internet Banking/input_M-PIN_otpToken'), '111111')
            
            WebUI.setEncryptedText(findTestObject('Object Repository/m_bsb_lain/Page_Bank Sumsel Babel - Internet Banking/input_M-PIN_otpPin'), 'SFTQUhjBfIY=')
            
            WebUI.click(findTestObject('Object Repository/m_bsb_lain/Page_Bank Sumsel Babel - Internet Banking/input_Untuk meminta M-PIN yang baru, silaka_f72c0a'))
            
            WebUI.delay(3)
            
            // Cek hasil akhir
            boolean finalSuccess = WebUI.verifyTextPresent('berhasil', false, FailureHandling.OPTIONAL) ||
                                   WebUI.verifyTextPresent('sukses', false, FailureHandling.OPTIONAL) ||
                                   WebUI.verifyTextPresent('telah dibuat', false, FailureHandling.OPTIONAL)
            
            boolean finalFailed = WebUI.verifyTextPresent('gagal', false, FailureHandling.OPTIONAL) ||
                                  WebUI.verifyTextPresent('Transaksi anda gagal dilakukan', false, FailureHandling.OPTIONAL)
            
            if (finalSuccess && !finalFailed) {
                actualCode = "00"
                actualMessage = "Approved"
            } else if (finalFailed) {
                actualCode = "99"
                actualMessage = "Transaction Failed"
            } else {
                actualCode = "99"
                actualMessage = "Unknown Status"
            }
            
            WebUI.comment("→ Actual Response: ${actualCode} - ${actualMessage}")
        } else {
            actualCode = "99"
            actualMessage = "M-PIN Page Not Loaded"
            WebUI.comment("→ Actual Response: ${actualCode} - ${actualMessage}")
        }
    }
    
    totalProcessed++
    
    // ===== COMPARE EXPECTED vs ACTUAL =====
    
    boolean isMatch = (expectedCode == actualCode)
    
    if (isMatch) {
        matchCount++
        WebUI.comment("✓ MATCH: Expected ${expectedCode} = Actual ${actualCode}")
    } else {
        mismatchCount++
        WebUI.comment("✗ MISMATCH: Expected ${expectedCode} ≠ Actual ${actualCode}")
        WebUI.comment("  Expected: ${expectedMessage}")
        WebUI.comment("  Actual: ${actualMessage}")
        WebUI.comment("  Issue: ${issueDetected}")
        
        // Take screenshot
        def timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date())
        def screenshotName = "mismatch_${transaksi_id}_${timestamp}.png"
        def screenshotPath = WebUI.takeScreenshot(screenshotName)
        
        // Add to Excel Report
        def dataRow = sheet.createRow(mismatchRowNum++)
        dataRow.createCell(0).setCellValue(mismatchCount)
        dataRow.createCell(1).setCellValue(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))
        dataRow.createCell(2).setCellValue(transaksi_id.toString())
        dataRow.createCell(3).setCellValue(rek_sumber.toString())
        dataRow.createCell(4).setCellValue(rekSumberFound ? "YES" : "NO")
        dataRow.createCell(5).setCellValue(rek_tujuan.toString())
        dataRow.createCell(6).setCellValue(rekTujuanFound ? "YES" : "NO")
        dataRow.createCell(7).setCellValue(sejumlah.toString())
        dataRow.createCell(8).setCellValue(kodifikasi.toString())
        dataRow.createCell(9).setCellValue(kodifikasiValid ? "YES" : "NO (" + kodifikasiIssue + ")")
        dataRow.createCell(10).setCellValue(expectedCode)
        dataRow.createCell(11).setCellValue(expectedMessage)
        dataRow.createCell(12).setCellValue(actualCode)
        dataRow.createCell(13).setCellValue(actualMessage)
        dataRow.createCell(14).setCellValue(issueDetected)
        dataRow.createCell(15).setCellValue(screenshotPath)
        
        WebUI.comment("→ Mismatch logged to Excel report")
    }

    WebUI.delay(2)
    WebUI.comment("✓ Transaksi ${transaksi_id} selesai diproses")
    //(/transaksi)
}

//batas looping dan summary
WebUI.comment("")
WebUI.comment("==========================================")
WebUI.comment("=== SUMMARY REPORT ===")
WebUI.comment("==========================================")
WebUI.comment("Total transaksi diproses: ${totalProcessed}")
WebUI.comment("MATCH: ${matchCount} (${totalProcessed > 0 ? String.format('%.1f', matchCount * 100.0 / totalProcessed) : '0'}%)")
WebUI.comment("MISMATCH: ${mismatchCount} (${totalProcessed > 0 ? String.format('%.1f', mismatchCount * 100.0 / totalProcessed) : '0'}%)")
WebUI.comment("")

// Save Excel Report
if (mismatchCount > 0) {
    // Auto-adjust column width
    for (int col = 0; col < headers.size(); col++) {
        sheet.autoSizeColumn(col)
    }
    
    // Save file
    def reportTimestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date())
    def reportFileName = "Mismatch_Report_DanaBOS_ISO8583_${reportTimestamp}.xlsx"
    def reportPath = "E:\\Folder Internship\\report_automation\\${reportFileName}"
    
    def reportDir = new File("E:\\Folder Internship\\report_automation")
    if (!reportDir.exists()) {
        reportDir.mkdirs()
    }
    
    def fileOut = new FileOutputStream(reportPath)
    workbook.write(fileOut)
    fileOut.close()
    workbook.close()
    
    WebUI.comment("==========================================")
    WebUI.comment("✓ Mismatch Report saved to:")
    WebUI.comment("  ${reportPath}")
    WebUI.comment("==========================================")
} else {
    workbook.close()
    WebUI.comment("✓ No mismatch found - Excel report not generated")
}

// Final Assessment
if (mismatchCount == 0 && totalProcessed > 0) {
    WebUI.comment("")
    WebUI.comment("✓✓✓ ALL TESTS PASSED - 100% MATCH ✓✓✓")
} else if (mismatchCount > 0) {
    WebUI.comment("")
    WebUI.comment("⚠⚠⚠ ${mismatchCount} MISMATCH FOUND - Check Excel Report ⚠⚠⚠")
}

WebUI.comment("==========================================")

WebUI.comment("=== Semua transaksi selesai ===")
WebUI.click(findTestObject('Object Repository/m_bsb_lain/Page_Bank Sumsel Babel - Internet Banking/a_Keluar'))
WebUI.delay(2)
WebUI.closeBrowser()
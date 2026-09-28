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
    'APPROVE0001')

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
// SCRIPT APPROVAL TRANSAKSI - SMART MATCHING
// ========================================

// Load data dari Excel
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

WebUI.comment("Total transaksi di Excel: ${totalRows}")

// Buat list transaksi dari Excel
List<Map> excelTransactions = []
for (int i = 1; i <= totalRows; i++) {
    def transaksi_id = allData.getValue('transaksi_id', i)
    def rek_sumber = allData.getValue('rek_sumber', i).toString().trim()
    def rek_tujuan = allData.getValue('rek_tujuan', i).toString().trim()
    def sejumlah = allData.getValue('sejumlah', i).toString().trim()
    def catatan = allData.getValue('catatan', i).toString().trim()
    
    excelTransactions.add([
        transaksi_id: transaksi_id,
        rek_sumber: rek_sumber,
        rek_tujuan: rek_tujuan,
        sejumlah: sejumlah,
        catatan: catatan,
        found: false,  // tracking apakah sudah ditemukan
        approved: false // tracking apakah sudah di-approve
    ])
}

WebUI.comment("List transaksi Excel created: ${excelTransactions.size()} entries")

// Navigasi ke halaman approval
WebUI.click(findTestObject('Object Repository/m_approval/Page_Bank Sumsel Babel - Internet Banking/a_Ke Rekening Bank Sumsel Babel Lain'))

WebUI.waitForPageLoad(30)

// Statistics
int processedCount = 0
int approvedCount = 0
int skippedCount = 0
int rejectedCount = 0
int totalPagesScanned = 0

// Loop semua halaman sampai semua transaksi Excel ditemukan
boolean hasMorePages = true
int currentPage = 1
int maxPages = 10

while (hasMorePages && currentPage <= maxPages) {
    WebUI.comment("=== Scanning Halaman ${currentPage} ===")
    totalPagesScanned++
    
    WebUI.delay(2)
    
    // Ambil semua row transaksi dari tabel di halaman ini
    def tableRows = WebUI.executeJavaScript("""
        var rows = [];
        var table = document.querySelector('table');
        if (!table) return rows;
        
        var tbody = table.querySelector('tbody');
        if (!tbody) tbody = table;
        
        var trs = tbody.querySelectorAll('tr');
        
        for (var i = 0; i < trs.length; i++) {
            var cells = trs[i].querySelectorAll('td');
            if (cells.length >= 6) {
                rows.push({
                    index: i,
                    rek_sumber: cells[0].textContent.trim(),
                    rek_tujuan: cells[1].textContent.trim(),
                    jumlah: cells[2].textContent.trim(),
                    catatan: cells[3].textContent.trim(),
                    status: cells[4].textContent.trim(),
                    rilisLink: cells[5].querySelector('a') ? cells[5].querySelector('a').href : null
                });
            }
        }
        return rows;
    """, null)
    
    WebUI.comment("Total transaksi di halaman ini: ${tableRows.size()}")
    
    if (tableRows.size() == 0) {
        WebUI.comment("Tidak ada transaksi di halaman ini - Stop scanning")
        break
    }
    
    // Loop setiap transaksi di Excel, cari di halaman web ini
    for (int excelIdx = 0; excelIdx < excelTransactions.size(); excelIdx++) {
        def excelTrx = excelTransactions[excelIdx]
        
        // Skip jika sudah di-approve
        if (excelTrx.approved) {
            continue
        }
        
        WebUI.comment("→ Mencari transaksi ${excelTrx.transaksi_id} di halaman ${currentPage}")
        
        // Cari di halaman web
        boolean foundInThisPage = false
        def matchedWebRow = null
        
        for (int webIdx = 0; webIdx < tableRows.size(); webIdx++) {
            def webRow = tableRows[webIdx]
            
            // Convert format jumlah dari web ke format Excel
            String jumlahWeb = webRow.jumlah.replaceAll('[.]', '').replaceAll(',.*', '').trim()
            
            // Cek kecocokan 4 field
            boolean isMatch = (webRow.rek_sumber == excelTrx.rek_sumber &&
                              webRow.rek_tujuan == excelTrx.rek_tujuan &&
                              jumlahWeb == excelTrx.sejumlah &&
                              webRow.catatan == excelTrx.catatan)
            
            if (isMatch) {
                WebUI.comment("✓ MATCH ditemukan! ${excelTrx.transaksi_id} di row ${webIdx + 1}")
                foundInThisPage = true
                matchedWebRow = webRow
                excelTrx.found = true
                break
            }
        }
        
        // Jika tidak ditemukan di halaman ini, lanjut ke transaksi Excel berikutnya
        if (!foundInThisPage) {
            continue
        }
        
        // Transaksi ditemukan - proses approval
        processedCount++
        WebUI.comment("=== Processing transaksi ${processedCount}: ${excelTrx.transaksi_id} ===")
        WebUI.comment("Data: ${excelTrx.rek_sumber} → ${excelTrx.rek_tujuan} | ${excelTrx.sejumlah} | ${excelTrx.catatan}")
        
        // Klik link Rilis
        WebUI.navigateToUrl(matchedWebRow.rilisLink)
        WebUI.delay(2)
        
        // Validasi detail di halaman approval menggunakan JavaScript
        def details = WebUI.executeJavaScript("""
            var fieldsets = document.querySelectorAll('fieldset');
            
            // Fieldset 1: Informasi Rekening Sumber
            var fs1 = fieldsets[0].querySelector('table');
            var rek_sumber = fs1.querySelector('tr:nth-child(1) td:nth-child(2)').textContent.trim();
            
            // Fieldset 2: Informasi Rekening Tujuan
            var fs2 = fieldsets[1].querySelector('table');
            var rek_tujuan = fs2.querySelector('tr:nth-child(1) td:nth-child(2)').textContent.trim();
            
            // Fieldset 3: Informasi Pemindahbukuan
            var fs3 = fieldsets[2].querySelector('table');
            var jumlah = fs3.querySelector('tr:nth-child(1) td:nth-child(2)').textContent.trim();
            var catatan = fs3.querySelector('tr:nth-child(3) td:nth-child(2)').textContent.trim();
            
            return {
                rek_sumber: rek_sumber,
                rek_tujuan: rek_tujuan,
                jumlah: jumlah,
                catatan: catatan
            };
        """, null)
        
        def detailRekSumber = details.rek_sumber
        def detailRekTujuan = details.rek_tujuan
        def detailJumlah = details.jumlah
        def detailCatatan = details.catatan
        
        // Convert format jumlah detail
        String jumlahDetail = detailJumlah.replaceAll('[.]', '').replaceAll(',.*', '').trim()
        
        // Validasi kecocokan
        boolean isValid = true
        String errorMsg = ""
        
        if (detailRekSumber != excelTrx.rek_sumber) {
            isValid = false
            errorMsg += "- Rekening Sumber tidak cocok\n  Web: ${detailRekSumber}\n  Excel: ${excelTrx.rek_sumber}\n"
        }
        
        if (detailRekTujuan != excelTrx.rek_tujuan) {
            isValid = false
            errorMsg += "- Rekening Tujuan tidak cocok\n  Web: ${detailRekTujuan}\n  Excel: ${excelTrx.rek_tujuan}\n"
        }
        
        if (jumlahDetail != excelTrx.sejumlah) {
            isValid = false
            errorMsg += "- Jumlah tidak cocok\n  Web: ${jumlahDetail}\n  Excel: ${excelTrx.sejumlah}\n"
        }
        
        if (detailCatatan != excelTrx.catatan) {
            isValid = false
            errorMsg += "- Catatan tidak cocok\n  Web: ${detailCatatan}\n  Excel: ${excelTrx.catatan}\n"
        }
        
        if (!isValid) {
            WebUI.comment("⚠ Validasi detail gagal!")
            WebUI.takeScreenshot()
            
            // Pop up konfirmasi
            int choice = JOptionPane.showOptionDialog(
                null,
                "Validasi detail transaksi GAGAL:\n\n${errorMsg}\nApa yang ingin dilakukan?",
                "Validasi Gagal - ${excelTrx.transaksi_id}",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.ERROR_MESSAGE,
                null,
                ["Skip", "Tolak", "Stop"] as Object[],
                "Tolak"
            )
            
            if (choice == 0) {
                // Skip
                WebUI.comment("→ User memilih Skip")
                skippedCount++
                WebUI.back()
                WebUI.delay(2)
                continue
            } else if (choice == 1) {
                // Tolak
                WebUI.comment("→ User memilih Tolak")
                WebUI.click(findTestObject('Object Repository/m_approval/Page_Bank Sumsel Babel - Internet Banking/input_Untuk meminta M-PIN yang baru, silaka_be26fa'))
                WebUI.delay(1)
                rejectedCount++
                excelTrx.approved = true // Mark sebagai processed
                WebUI.back()
                WebUI.delay(2)
                continue
            } else {
                // Stop
                WebUI.comment("→ User memilih Stop - Menghentikan eksekusi")
                hasMorePages = false
                break
            }
        }
        
        // Validasi OK - Approve transaksi
        WebUI.comment("✓ Validasi detail berhasil - Melakukan approval")
        
        // Input M-PIN
        WebUI.setText(findTestObject('Object Repository/m_approval/Page_Bank Sumsel Babel - Internet Banking/input_M-PIN_otpToken'), '111111')
        
        WebUI.setEncryptedText(findTestObject('Object Repository/m_approval/Page_Bank Sumsel Babel - Internet Banking/input_M-PIN_otpPin'), 'SFTQUhjBfIY=')
        
        // Klik Rilis
        WebUI.click(findTestObject('Object Repository/m_approval/Page_Bank Sumsel Babel - Internet Banking/input_Untuk meminta M-PIN yang baru, silaka_6ab805'))
        
        WebUI.delay(2)
        
        // Cek apakah berhasil
        boolean success = WebUI.verifyTextPresent('berhasil', false, FailureHandling.OPTIONAL) ||
                          WebUI.verifyTextPresent('sukses', false, FailureHandling.OPTIONAL)
        
        if (success) {
            WebUI.comment("✓ Transaksi ${excelTrx.transaksi_id} berhasil di-approve")
            approvedCount++
            excelTrx.approved = true
        } else {
            WebUI.comment("⚠ Status approval tidak jelas")
            WebUI.takeScreenshot()
        }
        
        // Kembali ke halaman daftar
        WebUI.back()
        WebUI.delay(2)
    }
    
    // Early termination - cek apakah semua transaksi Excel sudah di-approve
    int approvedTotal = excelTransactions.count { it.approved }
    WebUI.comment("Progress: ${approvedTotal}/${excelTransactions.size()} transaksi di-approve")
    
    if (approvedTotal >= excelTransactions.size()) {
        WebUI.comment("✓ Semua transaksi dari Excel sudah di-approve - Stop scanning")
        break
    }
    
    // Cek apakah ada halaman berikutnya
    boolean hasNextPage = WebUI.executeJavaScript("""
        var nextButton = document.querySelector('input[value=">>"]');
        return nextButton && !nextButton.disabled;
    """, null)
    
    if (hasNextPage && currentPage < maxPages) {
        WebUI.comment("→ Navigasi ke halaman ${currentPage + 1}")
        WebUI.click(findTestObject('Object Repository/m_approval/Page_Bank Sumsel Babel - Internet Banking/a_'))
        currentPage++
        WebUI.delay(2)
    } else {
        WebUI.comment("→ Tidak ada halaman berikutnya - Stop scanning")
        hasMorePages = false
    }
}

// Summary
WebUI.comment("=== SUMMARY ===")
WebUI.comment("Total transaksi di Excel: ${excelTransactions.size()}")
WebUI.comment("Total halaman di-scan: ${totalPagesScanned}")
WebUI.comment("Processed: ${processedCount}")
WebUI.comment("Approved: ${approvedCount}")
WebUI.comment("Skipped: ${skippedCount}")
WebUI.comment("Rejected: ${rejectedCount}")

// Warning - list transaksi yang tidak ditemukan di web
int notFoundCount = excelTransactions.count { !it.found }
if (notFoundCount > 0) {
    WebUI.comment("⚠ WARNING: ${notFoundCount} transaksi dari Excel TIDAK ditemukan di web:")
    
    excelTransactions.each { trx ->
        if (!trx.found) {
            WebUI.comment("  - ${trx.transaksi_id}: ${trx.rek_sumber} → ${trx.rek_tujuan} | ${trx.sejumlah} | ${trx.catatan}")
        }
    }
}

// Warning - list transaksi yang ditemukan tapi tidak di-approve
int foundButNotApproved = excelTransactions.count { it.found && !it.approved }
if (foundButNotApproved > 0) {
    WebUI.comment("⚠ WARNING: ${foundButNotApproved} transaksi ditemukan tapi TIDAK di-approve:")
    
    excelTransactions.each { trx ->
        if (trx.found && !trx.approved) {
            WebUI.comment("  - ${trx.transaksi_id}: Skipped atau Rejected")
        }
    }
}

// Logout
WebUI.click(findTestObject('Object Repository/m_approval/Page_Bank Sumsel Babel - Internet Banking/a_Keluar'))
WebUI.delay(2)
WebUI.closeBrowser()
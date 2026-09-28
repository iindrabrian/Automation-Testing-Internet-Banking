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

WebUI.openBrowser('')

WebUI.navigateToUrl('http://139.255.78.230:12080/corp/index.html')

WebUI.setText(findTestObject('Object Repository/m_approval/Page_Bank Sumsel Babel - Internet Banking/input_Masukkan ID Perusahaan Anda_loginCorp_6066dd'), 
    'RKU0862C')

WebUI.setText(findTestObject('Object Repository/m_approval/Page_Bank Sumsel Babel - Internet Banking/input_Silakan masukkan User ID anda_loginCo_3c7140'), 
    'APPROVE0001')

WebUI.setText(findTestObject('Object Repository/m_approval/Page_Bank Sumsel Babel - Internet Banking/input_Ketik code yang muncul digambar_login_fc3d9b'), 
    'sd')

WebUI.sendKeys(findTestObject('Object Repository/m_approval/Page_Bank Sumsel Babel - Internet Banking/input_Ketik code yang muncul digambar_login_fc3d9b'), 
    Keys.chord(Keys.ENTER))

WebUI.setEncryptedText(findTestObject('Object Repository/m_approval/Page_Bank Sumsel Babel - Internet Banking/input_Silakan masukkan password anda_textfield'), 
    '7OEX3bziT2+5Gr57kyA9WA==')

WebUI.sendKeys(findTestObject('Object Repository/m_approval/Page_Bank Sumsel Babel - Internet Banking/input_Silakan masukkan password anda_textfield'), 
    Keys.chord(Keys.ENTER))

WebUI.click(findTestObject('Object Repository/m_approval/Page_Bank Sumsel Babel - Internet Banking/a_Ke Rekening Bank Sumsel Babel Lain'))

WebUI.click(findTestObject('Object Repository/m_approval/Page_Bank Sumsel Babel - Internet Banking/fieldset_Daftar TransferRekening SumberReke_a41204'))

WebUI.comment('approve process')

WebUI.click(findTestObject('Object Repository/m_approval/Page_Bank Sumsel Babel - Internet Banking/a_Rilis'))

WebUI.setText(findTestObject('Object Repository/m_approval/Page_Bank Sumsel Babel - Internet Banking/input_M-PIN_otpToken'), 
    '111111')

WebUI.setEncryptedText(findTestObject('Object Repository/m_approval/Page_Bank Sumsel Babel - Internet Banking/input_M-PIN_otpPin'), 
    'SFTQUhjBfIY=')

WebUI.click(findTestObject('Object Repository/m_approval/Page_Bank Sumsel Babel - Internet Banking/input_Untuk meminta M-PIN yang baru, silaka_6ab805'))

WebUI.click(findTestObject('Object Repository/m_approval/Page_Bank Sumsel Babel - Internet Banking/input_Untuk meminta M-PIN yang baru, silaka_be26fa'))

WebUI.click(findTestObject(null))

WebUI.closeBrowser()

WebUI.getText(findTestObject(null))


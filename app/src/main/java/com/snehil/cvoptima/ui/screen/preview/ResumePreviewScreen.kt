package com.snehil.cvoptima.ui.screen.preview

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.snehil.cvoptima.core.navigation.Screen
import com.snehil.cvoptima.core.util.PdfAtsAnalyzer
import com.snehil.cvoptima.data.local.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumePreviewScreen(
    navController: NavController,
    viewModel: ResumePreviewViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val loading by viewModel.loading.collectAsState()
    val exporting by viewModel.exporting.collectAsState()
    val scope = rememberCoroutineScope()

    var showAtsSheet by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }
    var scanCompleted by remember { mutableStateOf(false) }
    var scanStatusText by remember { mutableStateOf("Initializing AI Scanner...") }

    var selectedFileName by remember { mutableStateOf("") }
    var isScanningUploadedFile by remember { mutableStateOf(false) }
    var showUploadedReport by remember { mutableStateOf(false) }
    var uploadScanStatusText by remember { mutableStateOf("") }
    var uploadedAtsResult by remember { mutableStateOf<PdfAtsAnalyzer.AtsAnalysisResult?>(null) }
    var isUploadedAiPowered by remember { mutableStateOf(false) }

    val fileLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            var name = "resume.pdf"
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        name = it.getString(index)
                    }
                }
            }
            selectedFileName = name
            isScanningUploadedFile = true
            showUploadedReport = false
            
            scope.launch {
                uploadScanStatusText = "Extracting text from PDF..."
                
                // Try to extract readable text from the PDF bytes
                var extractedText = withContext(Dispatchers.Default) {
                    try {
                        val bytes = withContext(Dispatchers.IO) {
                            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        } ?: byteArrayOf()
                        // Extract printable ASCII/UTF-8 text from raw PDF bytes
                        extractTextFromPdfBytes(bytes)
                    } catch (e: Exception) {
                        Log.e("ATS_FLOW", "Digital text extraction failed", e)
                        ""
                    }
                }

                // Determine if digitally extracted text is too short or appears to be scrambled/gibberish (custom font/CMap encoding)
                val isTextValid = extractedText.length >= 50 && run {
                    val lower = extractedText.lowercase()
                    lower.contains("experience") || lower.contains("work") ||
                    lower.contains("education") || lower.contains("degree") ||
                    lower.contains("skills") || lower.contains("technologies") ||
                    lower.contains("projects") || lower.contains("profile") ||
                    lower.contains("contact")
                }

                if (!isTextValid) {
                    Log.i("ATS_FLOW", "Digital text extraction returned short or scrambled text (length: ${extractedText.length}). Attempting ML Kit OCR extraction...")
                    uploadScanStatusText = "Running OCR scan on pages..."
                    val ocrText = withContext(Dispatchers.Default) {
                        try {
                            extractTextViaOcr(context, uri)
                        } catch (e: Exception) {
                            Log.e("ATS_FLOW", "ML Kit OCR text extraction failed", e)
                            ""
                        }
                    }
                    if (ocrText.length >= 50) {
                        Log.i("ATS_FLOW", "ML Kit OCR successfully extracted ${ocrText.length} characters.")
                        extractedText = ocrText
                    } else {
                        Log.w("ATS_FLOW", "ML Kit OCR extraction failed or returned too little text (${ocrText.length} chars).")
                    }
                }

                if (extractedText.length > 50) {
                    // Enough text extracted — send to AI backend
                    uploadScanStatusText = "AI is analyzing your resume..."
                    delay(500)
                    uploadScanStatusText = "Evaluating content & structure..."
                    
                    val aiResponse = viewModel.analyzeTextWithAi(extractedText)
                    
                    if (aiResponse != null) {
                        uploadedAtsResult = PdfAtsAnalyzer.AtsAnalysisResult(
                            score = aiResponse.score,
                            rating = aiResponse.rating,
                            layoutSuggestions = aiResponse.layoutSuggestions ?: emptyList(),
                            keywordSuggestions = aiResponse.keywordSuggestions ?: emptyList(),
                            metricSuggestions = aiResponse.metricSuggestions ?: emptyList(),
                            verbSuggestions = aiResponse.verbSuggestions ?: emptyList()
                        )
                        isUploadedAiPowered = true
                        Log.i("ATS_FLOW", "ATS COMPATIBILITY RESULT (AI-POWERED): $uploadedAtsResult")
                        isScanningUploadedFile = false
                        showUploadedReport = true
                        return@launch
                    } else {
                        Log.w("ATS_FLOW", "AI ATS analysis returned null response, falling back to local scan.")
                    }
                } else {
                    Log.i("ATS_FLOW", "Extracted text length (${extractedText.length}) too short for AI analysis. Falling back to local scan.")
                }
                
                // Fallback to local pixel-based analysis
                uploadScanStatusText = "Running local ATS scan..."
                delay(400)
                uploadScanStatusText = "Scanning layout & formatting..."
                val result = PdfAtsAnalyzer.analyze(context, uri, name)
                uploadedAtsResult = result
                isUploadedAiPowered = false
                Log.i("ATS_FLOW", "ATS COMPATIBILITY RESULT (LOCAL FALLBACK): $uploadedAtsResult")
                isScanningUploadedFile = false
                showUploadedReport = true
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Resume Preview",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (exporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        IconButton(
                            onClick = {
                                viewModel.exportPdf(context) { success ->
                                    val msg = if (success) "PDF exported to Downloads folder!" else "Failed to export PDF"
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Export PDF")
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    // 1. Resume Sheet (A4 Mimic View)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .shadow(8.dp, RoundedCornerShape(16.dp))
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            color = Color.White
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(24.dp)
                            ) {
                                ResumeDocumentContent(viewModel = viewModel)
                            }
                        }
                    }

                    // 2. Action Controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    showAtsSheet = true
                                    isScanning = true
                                    scanCompleted = false
                                    
                                    scanStatusText = "Connecting to AI Engine..."
                                    // Trigger AI analysis in background
                                    viewModel.analyzeProfileWithAi()
                                    
                                    scanStatusText = "AI is reading your resume..."
                                    delay(1200)
                                    scanStatusText = "Evaluating keywords & structure..."
                                    delay(1000)
                                    scanStatusText = "Generating improvement suggestions..."
                                    
                                    // Wait for AI analysis to finish
                                    viewModel.aiAnalysisLoading.collect { loading ->
                                        if (!loading) {
                                            isScanning = false
                                            scanCompleted = true
                                            return@collect
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Analyze ATS Compatibility",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp)
                            )
                        }
                    }
                }
            }
        }
    }

    // 3. ATS Analysis Bottom Sheet
    if (showAtsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAtsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 620.dp)
                    .padding(bottom = 24.dp)
            ) {
                if (isScanning) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(64.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 5.dp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = scanStatusText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "OpenAI is analyzing your resume content for ATS compatibility.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (scanCompleted) {
                    AtsReportCardView(
                        viewModel = viewModel,
                        onDismiss = { showAtsSheet = false },
                        selectedFileName = selectedFileName,
                        isScanningUploadedFile = isScanningUploadedFile,
                        showUploadedReport = showUploadedReport,
                        scanStatusText = uploadScanStatusText,
                        atsResult = uploadedAtsResult,
                        isUploadedAiPowered = isUploadedAiPowered,
                        onUploadClick = { fileLauncher.launch("application/pdf") },
                        onShowUploadedReportChange = { showUploadedReport = it }
                    )
                }
            }
        }
    }
}

@Composable
fun ResumeDocumentContent(viewModel: ResumePreviewViewModel) {
    val info = viewModel.basicInfo ?: return
    val email = info.email ?: ""
    val contact = info.contactNumber ?: ""
    val location = info.location ?: ""
    val linkedin = info.linkedinUrl ?: ""
    val github = info.githubUrl ?: ""
    val portfolio = info.portfolioUrl ?: ""

    // Header (Centered, matching user's layout)
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = (info.name ?: "CANDIDATE NAME").uppercase(),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                fontFamily = FontFamily.Serif,
                letterSpacing = 1.sp
            ),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(4.dp))

        // Row of contact options
        val contactItems = listOfNotNull(
            if (email.isNotEmpty()) email else null,
            if (contact.isNotEmpty()) contact else null,
            if (linkedin.isNotEmpty()) "LinkedIn" else null,
            if (github.isNotEmpty()) "GitHub" else null,
            if (portfolio.isNotEmpty()) "Portfolio" else null,
            if (location.isNotEmpty()) location else null
        )
        
        Text(
            text = contactItems.joinToString("   •   "),
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color.DarkGray,
                fontFamily = FontFamily.Serif,
                fontSize = 10.5.sp
            ),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Profile summary
    if (!info.professionalSummary.isNullOrBlank()) {
        ResumeSectionHeader(title = "Profile")
        Text(
            text = info.professionalSummary,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color.Black,
                fontFamily = FontFamily.Serif,
                fontSize = 11.sp,
                lineHeight = 16.sp
            ),
            modifier = Modifier.padding(bottom = 12.dp)
        )
    }

    // Dynamic Sections based on order settings
    viewModel.sectionOrder.forEach { section ->
        when (section) {
            "Skills" -> {
                if (viewModel.skillGroups.isNotEmpty()) {
                    ResumeSectionHeader(title = "Technical Skills")
                    Column(modifier = Modifier.padding(bottom = 12.dp)) {
                        viewModel.skillGroups.forEach { group ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 1.dp)
                            ) {
                                Text(
                                    text = "${group.label}: ",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black,
                                        fontFamily = FontFamily.Serif,
                                        fontSize = 11.sp
                                    )
                                )
                                Text(
                                    text = group.skills.joinToString(", "),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color.Black,
                                        fontFamily = FontFamily.Serif,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
            "Work Experiences", "Work Experience" -> {
                if (viewModel.experiences.isNotEmpty()) {
                    ResumeSectionHeader(title = "Professional Experience")
                    Column(modifier = Modifier.padding(bottom = 12.dp)) {
                        viewModel.experiences.forEach { exp ->
                            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = exp.company + (if (!exp.location.isNullOrBlank()) " (${exp.location})" else ""),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black,
                                            fontFamily = FontFamily.Serif,
                                            fontSize = 11.5.sp
                                        )
                                    )
                                    Text(
                                        text = "${exp.startDate ?: ""} – ${exp.endDate ?: ""}",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Medium,
                                            color = Color.Black,
                                            fontFamily = FontFamily.Serif,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                                Text(
                                    text = exp.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        color = Color.DarkGray,
                                        fontFamily = FontFamily.Serif,
                                        fontSize = 11.sp
                                    ),
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                                // Bullet points
                                exp.bulletPoints?.forEach { bullet ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 8.dp, bottom = 1.dp)
                                    ) {
                                        Text(
                                            text = "• ",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = Color.Black,
                                                fontFamily = FontFamily.Serif,
                                                fontSize = 11.sp
                                            )
                                        )
                                        Text(
                                            text = bullet,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = Color.Black,
                                                fontFamily = FontFamily.Serif,
                                                fontSize = 11.sp,
                                                lineHeight = 15.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            "Projects" -> {
                if (viewModel.projects.isNotEmpty()) {
                    ResumeSectionHeader(title = "Projects")
                    Column(modifier = Modifier.padding(bottom = 12.dp)) {
                        viewModel.projects.forEach { proj ->
                            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = proj.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black,
                                            fontFamily = FontFamily.Serif,
                                            fontSize = 11.5.sp
                                        )
                                    )
                                    Text(
                                        text = proj.date ?: "",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Medium,
                                            color = Color.Black,
                                            fontFamily = FontFamily.Serif,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                                
                                // GitHub / Tech stack line
                                var subline = ""
                                if (!proj.techStack.isNullOrBlank()) {
                                    subline += "[Tech: ${proj.techStack}]"
                                }
                                if (!proj.link.isNullOrBlank()) {
                                    if (subline.isNotEmpty()) subline += " | "
                                    subline += "Link: ${proj.link}"
                                }
                                if (subline.isNotEmpty()) {
                                    Text(
                                        text = subline,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color.DarkGray,
                                            fontFamily = FontFamily.Serif,
                                            fontSize = 10.sp
                                        ),
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }

                                // Bullet points
                                proj.bulletPoints?.forEach { bullet ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 8.dp, bottom = 1.dp)
                                    ) {
                                        Text(
                                            text = "• ",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = Color.Black,
                                                fontFamily = FontFamily.Serif,
                                                fontSize = 11.sp
                                            )
                                        )
                                        Text(
                                            text = bullet,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = Color.Black,
                                                fontFamily = FontFamily.Serif,
                                                fontSize = 11.sp,
                                                lineHeight = 15.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            "Certifications" -> {
                if (viewModel.certifications.isNotEmpty()) {
                    ResumeSectionHeader(title = "Certifications")
                    Column(modifier = Modifier.padding(bottom = 12.dp)) {
                        viewModel.certifications.forEach { cert ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cert.title + (if (!cert.issuer.isNullOrBlank()) " – ${cert.issuer}" else ""),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black,
                                            fontFamily = FontFamily.Serif,
                                            fontSize = 11.sp
                                        )
                                    )
                                    if (!cert.link.isNullOrBlank()) {
                                        Text(
                                            text = cert.link,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color.DarkGray,
                                                fontFamily = FontFamily.Serif,
                                                fontSize = 10.sp
                                            )
                                        )
                                    }
                                }
                                Text(
                                    text = cert.date ?: "",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = Color.Black,
                                        fontFamily = FontFamily.Serif,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
            "Education" -> {
                if (viewModel.educations.isNotEmpty()) {
                    ResumeSectionHeader(title = "Education")
                    Column(modifier = Modifier.padding(bottom = 12.dp)) {
                        viewModel.educations.forEach { edu ->
                            Column(modifier = Modifier.padding(bottom = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = edu.institution,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black,
                                            fontFamily = FontFamily.Serif,
                                            fontSize = 11.5.sp
                                        )
                                    )
                                    Text(
                                        text = "${edu.startDate ?: ""} – ${edu.endDate ?: ""}",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Medium,
                                            color = Color.Black,
                                            fontFamily = FontFamily.Serif,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val field = if (edu.fieldOfStudy.isNullOrBlank()) "" else " in ${edu.fieldOfStudy}"
                                    Text(
                                        text = "${edu.degree}$field" + (if (!edu.location.isNullOrBlank()) ", ${edu.location}" else ""),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                            color = Color.DarkGray,
                                            fontFamily = FontFamily.Serif,
                                            fontSize = 11.sp
                                        )
                                    )
                                    val scoreStr = edu.score ?: edu.gpa?.toString() ?: ""
                                    if (scoreStr.isNotEmpty()) {
                                        Text(
                                            text = "CGPA: $scoreStr",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black,
                                                fontFamily = FontFamily.Serif,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResumeSectionHeader(title: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                fontFamily = FontFamily.Serif,
                fontSize = 12.5.sp,
                letterSpacing = 0.5.sp
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.Black)
        )
        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
fun AtsReportCardView(
    viewModel: ResumePreviewViewModel,
    onDismiss: () -> Unit,
    selectedFileName: String,
    isScanningUploadedFile: Boolean,
    showUploadedReport: Boolean,
    scanStatusText: String,
    atsResult: PdfAtsAnalyzer.AtsAnalysisResult?,
    isUploadedAiPowered: Boolean,
    onUploadClick: () -> Unit,
    onShowUploadedReportChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedCategoryTab by remember { mutableStateOf(0) }
    val categories = listOf("Layout & Format", "Keywords", "Metrics & Impact", "Power Verbs")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        // Sheet Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ATS Compatibility Report",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (viewModel.isAiPowered) "Powered by OpenAI GPT" else "Local heuristic evaluation",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (isScanningUploadedFile) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(64.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 5.dp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = scanStatusText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Extracting text and structure from $selectedFileName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else if (showUploadedReport) {
            // Render the uploaded PDF report
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PDF ATS Report",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isUploadedAiPowered) "Powered by OpenAI GPT" else "Local heuristic evaluation",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = selectedFileName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(onClick = { onShowUploadedReportChange(false) }) {
                        Text("Show Profile Report", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val scoreColor = Color(0xFF0D9488)
                        val outlineColor = MaterialTheme.colorScheme.outlineVariant
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawArc(
                                color = outlineColor,
                                startAngle = 140f,
                                sweepAngle = 260f,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                            val currentScore = atsResult?.score ?: 85
                            drawArc(
                                color = scoreColor,
                                startAngle = 140f,
                                sweepAngle = (currentScore / 100f) * 260f,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Text(
                            text = "${atsResult?.score ?: 85}%",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = atsResult?.rating ?: "Highly ATS Compatible",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = atsResult?.layoutSuggestions?.firstOrNull()
                                ?: "PDF layout analysed successfully.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedButton(
                    onClick = onUploadClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upload Another PDF", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Uploaded PDF Suggestions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val result = atsResult
                val pdfSuggestions = if (result != null) {
                    buildList {
                        result.layoutSuggestions.forEach { add(Pair("Layout & Format", it)) }
                        result.keywordSuggestions.forEach { add(Pair("Keywords & Content", it)) }
                        result.metricSuggestions.forEach { add(Pair("Metrics & Impact", it)) }
                        result.verbSuggestions.forEach { add(Pair("Power Verbs", it)) }
                    }
                } else {
                    listOf(
                        Pair("ATS Parser Layout", "Unable to analyse PDF. Please ensure the file is a valid, non-encrypted PDF."),
                    )
                }

                pdfSuggestions.forEach { (cat, desc) ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = cat,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Circular score gauge and rating side-by-side
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gauge Canvas
                Box(
                    modifier = Modifier.size(90.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val scoreAnim = remember { Animatable(0f) }
                    LaunchedEffect(viewModel.atsScore) {
                        scoreAnim.animateTo(
                            targetValue = viewModel.atsScore.toFloat(),
                            animationSpec = tween(durationMillis = 1500, easing = DecelerateInterpolator().toEasing())
                        )
                    }

                    val scoreColor = when {
                        viewModel.atsScore >= 85 -> Color(0xFF0D9488) // Teal
                        viewModel.atsScore >= 70 -> Color(0xFFEAB308) // Yellow
                        else -> Color(0xFFEF4444) // Red
                    }

                    val outlineColor = MaterialTheme.colorScheme.outlineVariant

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Track
                        drawArc(
                            color = outlineColor,
                            startAngle = 140f,
                            sweepAngle = 260f,
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                        // Active Fill
                        drawArc(
                            color = scoreColor,
                            startAngle = 140f,
                            sweepAngle = (scoreAnim.value / 100f) * 260f,
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    
                    Text(
                        text = "${scoreAnim.value.toInt()}%",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                // Score details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = viewModel.atsRating,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (viewModel.atsScore >= 70) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "A score of 75+ is generally recommended to bypass typical Applicant Tracking Filters.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Upload PDF Button
            OutlinedButton(
                onClick = onUploadClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Upload & Analyze PDF Resume", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Suggestions Tabs
            TabRow(
                selectedTabIndex = selectedCategoryTab,
                containerColor = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                categories.forEachIndexed { index, name ->
                    Tab(
                        selected = selectedCategoryTab == index,
                        onClick = { selectedCategoryTab = index },
                        text = {
                            Text(
                                text = name.substringBefore(" "),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Content List
            Box(modifier = Modifier.weight(1f)) {
                val tipsList = when (selectedCategoryTab) {
                    0 -> viewModel.atsLayoutTips
                    1 -> viewModel.atsKeywordTips
                    2 -> viewModel.atsMetricTips
                    else -> viewModel.atsVerbTips
                }

                if (tipsList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF0D9488),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Perfect compliance found in this category!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tipsList.forEach { tip ->
                            val isPositive = tip.startsWith("Great:") || tip.startsWith("Perfect:")
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isPositive) Color(0xFFF0FDF4) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = if (isPositive) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (isPositive) Color(0xFF16A34A) else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = tip,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                        color = if (isPositive) Color(0xFF166534) else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Extension function to convert android interpolator to compose easing
fun android.view.animation.Interpolator.toEasing() = Easing { x -> getInterpolation(x) }
class DecelerateInterpolator : android.view.animation.DecelerateInterpolator()

/**
 * Extracts readable text from raw PDF bytes by scanning for text objects.
 * This is a best-effort extraction that works for most standard PDFs,
 * including compressed PDFs (decompressing FlateDecode streams on the fly).
 * Looks for text between BT/ET markers and parenthesized Tj/TJ operators.
 */
private fun extractTextFromPdfBytes(bytes: ByteArray): String {
    val content = String(bytes, Charsets.ISO_8859_1)
    val sb = StringBuilder()
    
    // 1. Find all streams, decompress them if FlateDecoded, and collect search text
    var index = 0
    val textBlocks = mutableListOf<String>()
    
    // Add raw content as fallback search space
    textBlocks.add(content)
    
    while (true) {
        val streamIdx = content.indexOf("stream", index)
        if (streamIdx == -1) break
        
        val endStreamIdx = content.indexOf("endstream", streamIdx)
        if (endStreamIdx == -1) break
        
        var startOffset = streamIdx + 6
        if (startOffset < content.length && content[startOffset] == '\r') {
            startOffset++
        }
        if (startOffset < content.length && content[startOffset] == '\n') {
            startOffset++
        }
        
        var endOffset = endStreamIdx
        if (endOffset > startOffset && content[endOffset - 1] == '\n') {
            endOffset--
        }
        if (endOffset > startOffset && content[endOffset - 1] == '\r') {
            endOffset--
        }
        
        if (endOffset > startOffset) {
            val dictStart = (streamIdx - 200).coerceAtLeast(0)
            val dictPart = content.substring(dictStart, streamIdx)
            val isFlate = dictPart.contains("/FlateDecode") || dictPart.contains("/Flate")
            
            val streamBytes = ByteArray(endOffset - startOffset)
            for (i in startOffset until endOffset) {
                streamBytes[i - startOffset] = content[i].code.toByte()
            }
            
            try {
                if (isFlate) {
                    val decompressed = decompressFlate(streamBytes)
                    textBlocks.add(String(decompressed, Charsets.ISO_8859_1))
                } else {
                    textBlocks.add(String(streamBytes, Charsets.ISO_8859_1))
                }
            } catch (e: Exception) {
                // Ignore decoding errors for corrupted streams
            }
        }
        
        index = endStreamIdx + 9
    }
    
    // 2. Extract text from Tj, TJ, ', and " operators across all collected text blocks
    val tjPattern = Regex("""(?:\(([^)]*)\)|<([0-9a-fA-F\s]+)>)\s*(?:Tj|'|")""")
    val tjArrayPattern = Regex("""\[([^]]*)\]\s*TJ""")
    
    for (block in textBlocks) {
        // Strategy 1: Tj, ', or " operators
        for (match in tjPattern.findAll(block)) {
            val parenText = match.groupValues[1]
            val hexText = match.groupValues[2]
            val decoded = when {
                parenText.isNotEmpty() -> decodeParenthesizedString(parenText)
                hexText.isNotEmpty() -> decodeHexString(hexText)
                else -> ""
            }
            if (decoded.isNotBlank()) {
                sb.append(decoded).append(" ")
            }
        }
        
        // Strategy 2: TJ operator
        for (match in tjArrayPattern.findAll(block)) {
            val arrayContent = match.groupValues[1]
            val tokenPattern = Regex("""\(([^)]*)\)|<([0-9a-fA-F\s]+)>|(-?\d+(?:\.\d+)?)""")
            for (tokenMatch in tokenPattern.findAll(arrayContent)) {
                val parenText = tokenMatch.groupValues[1]
                val hexText = tokenMatch.groupValues[2]
                val numberText = tokenMatch.groupValues[3]
                
                when {
                    parenText.isNotEmpty() -> {
                        sb.append(decodeParenthesizedString(parenText))
                    }
                    hexText.isNotEmpty() -> {
                        sb.append(decodeHexString(hexText))
                    }
                    numberText.isNotEmpty() -> {
                        val num = numberText.toFloatOrNull()
                        if (num != null && num <= -80f) {
                            sb.append(" ")
                        }
                    }
                }
            }
            sb.append(" ")
        }
    }
    
    // Clean up: remove non-printable characters and excessive whitespace
    val extracted = sb.toString()
        .replace(Regex("[^\\x20-\\x7E\\n\\r]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
    
    return extracted
}

private fun decompressFlate(compressedBytes: ByteArray): ByteArray {
    val inflater = java.util.zip.Inflater()
    inflater.setInput(compressedBytes)
    val outputStream = java.io.ByteArrayOutputStream(compressedBytes.size)
    val buffer = ByteArray(1024)
    try {
        while (!inflater.finished()) {
            val count = inflater.inflate(buffer)
            if (count == 0) {
                if (inflater.needsInput() || inflater.needsDictionary()) {
                    break
                }
            } else {
                outputStream.write(buffer, 0, count)
            }
        }
    } catch (e: Exception) {
        // Decompression failed, return partial or empty bytes
    } finally {
        inflater.end()
    }
    return outputStream.toByteArray()
}

private fun decodeHexString(hexString: String): String {
    val cleanHex = hexString.replace(Regex("\\s+"), "")
    if (cleanHex.isEmpty()) return ""
    
    val paddedHex = if (cleanHex.length % 2 != 0) cleanHex + "0" else cleanHex
    val bytes = ByteArray(paddedHex.length / 2)
    try {
        for (i in bytes.indices) {
            val index = i * 2
            val b = paddedHex.substring(index, index + 2).toInt(16).toByte()
            bytes[i] = b
        }
    } catch (e: Exception) {
        return ""
    }
    
    if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
        try {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
        } catch (e: Exception) {
            // ignore
        }
    }
    
    val decoded = String(bytes, Charsets.UTF_8)
    return if (decoded.any { it.isLetterOrDigit() || it.isWhitespace() }) {
        decoded
    } else {
        String(bytes, Charsets.ISO_8859_1)
    }
}

private fun decodeParenthesizedString(raw: String): String {
    val sb = StringBuilder()
    var i = 0
    while (i < raw.length) {
        val c = raw[i]
        if (c == '\\' && i + 1 < raw.length) {
            val next = raw[i + 1]
            if (next.isDigit()) {
                var octalVal = 0
                var len = 0
                while (len < 3 && i + 1 + len < raw.length && raw[i + 1 + len].isDigit()) {
                    val digit = raw[i + 1 + len] - '0'
                    if (digit in 0..7) {
                        octalVal = octalVal * 8 + digit
                        len++
                    } else {
                        break
                    }
                }
                sb.append(octalVal.toChar())
                i += 1 + len
            } else {
                when (next) {
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    'b' -> sb.append('\b')
                    'f' -> sb.append('\u000c')
                    '(' -> sb.append('(')
                    ')' -> sb.append(')')
                    '\\' -> sb.append('\\')
                    else -> sb.append(next)
                }
                i += 2
            }
        } else {
            sb.append(c)
            i++
        }
    }
    
    val result = sb.toString()
    if (result.length >= 2 && result[0].code == 0xFE && result[1].code == 0xFF) {
        try {
            val bytes = ByteArray(result.length)
            for (j in result.indices) {
                bytes[j] = result[j].code.toByte()
            }
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
        } catch (e: Exception) {
            // ignore
        }
    }
    return result
}

private fun extractTextViaOcr(context: Context, uri: Uri): String {
    val sb = StringBuilder()
    
    val pfd: ParcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r") 
        ?: return ""
        
    val renderer = try {
        PdfRenderer(pfd)
    } catch (e: Exception) {
        pfd.close()
        return ""
    }
    
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    val pageCount = renderer.pageCount
    
    for (i in 0 until pageCount) {
        val page = renderer.openPage(i)
        // Render PDF page to a Bitmap at 3x scale (approx. 216 DPI) for sharp OCR text recognition
        val scale = 3.0f
        val bmpWidth = (page.width * scale).toInt()
        val bmpHeight = (page.height * scale).toInt()
        val bmp = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        canvas.drawColor(android.graphics.Color.WHITE)
        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        
        // Process bitmap with ML Kit OCR
        try {
            val image = InputImage.fromBitmap(bmp, 0)
            val result = Tasks.await(recognizer.process(image))
            val text = result.text
            if (text.isNotBlank()) {
                sb.append(text).append("\n")
            }
        } catch (e: Exception) {
            Log.e("PdfOcr", "Failed to run OCR on page $i", e)
        } finally {
            bmp.recycle()
        }
    }
    
    renderer.close()
    pfd.close()
    
    return sb.toString().trim()
}

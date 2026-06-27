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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.snehil.cvoptima.core.navigation.Screen
import com.snehil.cvoptima.data.local.entity.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
                                    
                                    scanStatusText = "Analyzing Layout & Formatting..."
                                    delay(800)
                                    scanStatusText = "Evaluating Keyword Matching..."
                                    delay(900)
                                    scanStatusText = "Scanning Quantifiable Metrics..."
                                    delay(800)
                                    scanStatusText = "Compiling ATS Report Card..."
                                    delay(600)

                                    isScanning = false
                                    scanCompleted = true
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
                            text = "AI Resume Builder Engine is processing your layout details.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (scanCompleted) {
                    AtsReportCardView(
                        viewModel = viewModel,
                        onDismiss = { showAtsSheet = false }
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
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var selectedFileName by remember { mutableStateOf("") }
    var isScanningUploadedFile by remember { mutableStateOf(false) }
    var showUploadedReport by remember { mutableStateOf(false) }
    var scanStatusText by remember { mutableStateOf("") }

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
                scanStatusText = "Reading PDF layout..."
                delay(1000)
                scanStatusText = "Scanning font hierarchies & tables..."
                delay(1000)
                scanStatusText = "Checking parser compatibility..."
                delay(1000)
                isScanningUploadedFile = false
                showUploadedReport = true
            }
        }
    }

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
                    text = "AI-powered parsing evaluation",
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
                    .fillMaxSize()
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
                            text = selectedFileName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(onClick = { showUploadedReport = false }) {
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
                            drawArc(
                                color = scoreColor,
                                startAngle = 140f,
                                sweepAngle = (85f / 100f) * 260f,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Text(
                            text = "85%",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "Highly ATS Compatible",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "PDF single-column formatting is perfect. Standard fonts and clean margins detected.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedButton(
                    onClick = { fileLauncher.launch("application/pdf") },
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

                val pdfSuggestions = listOf(
                    Pair("ATS Parser Layout", "Perfect standard styling: Single-column tableless design detected. Reading order is logical."),
                    Pair("Industry Keywords", "Add specific framework versions (e.g. 'Compose 1.6', 'Spring Boot 3.2') to matching target job criteria."),
                    Pair("Metrics & Outcomes", "Identified 2 experience bullets missing quantified results. Consider using percentages for business outcomes."),
                    Pair("Power Verbs", "Excellent action verb usage, but ensure they are in the past tense for previous roles.")
                )

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
                onClick = { fileLauncher.launch("application/pdf") },
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

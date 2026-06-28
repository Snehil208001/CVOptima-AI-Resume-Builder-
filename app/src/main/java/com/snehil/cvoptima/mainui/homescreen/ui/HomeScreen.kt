package com.snehil.cvoptima.mainui.homescreen.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.snehil.cvoptima.core.navigation.Screen
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.snehil.cvoptima.core.util.PdfAtsAnalyzer
import com.snehil.cvoptima.domain.model.Document
import com.snehil.cvoptima.mainui.homescreen.viewmodel.HomeUiState
import com.snehil.cvoptima.mainui.homescreen.viewmodel.HomeViewModel
import com.snehil.cvoptima.ui.components.AppBottomNavigationBar
import com.snehil.cvoptima.data.remote.model.UserProfileDto
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    var showTailorDialog by remember { mutableStateOf(false) }
    var selectedDocument by remember { mutableStateOf<Document?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedFileUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var isScanningFile by remember { mutableStateOf(false) }
    var scanStatusText by remember { mutableStateOf("") }
    var showScanReport by remember { mutableStateOf(false) }
    var atsResult by remember { mutableStateOf<PdfAtsAnalyzer.AtsAnalysisResult?>(null) }
    val scope = rememberCoroutineScope()

    val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedFileUri = uri
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
            isScanningFile = true
            showScanReport = false
            
            scope.launch {
                scanStatusText = "Extracting text from PDF..."
                
                // Try to extract readable text from the PDF bytes
                var extractedText = withContext(Dispatchers.Default) {
                    try {
                        val bytes = withContext(Dispatchers.IO) {
                            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        } ?: byteArrayOf()
                        extractTextFromPdfBytes(bytes)
                    } catch (e: Exception) {
                        Log.e("ATS_FLOW", "Digital text extraction failed on home screen", e)
                        ""
                    }
                }

                // If digital text extraction is too short or scrambled, run OCR fallback
                val isTextValid = extractedText.length >= 50 && run {
                    val lower = extractedText.lowercase()
                    lower.contains("experience") || lower.contains("work") ||
                    lower.contains("education") || lower.contains("degree") ||
                    lower.contains("skills") || lower.contains("technologies") ||
                    lower.contains("projects") || lower.contains("profile") ||
                    lower.contains("contact")
                }

                if (!isTextValid) {
                    Log.i("ATS_FLOW", "Digital text extraction returned short/scrambled text. Attempting OCR...")
                    scanStatusText = "Running OCR scan on pages..."
                    val ocrText = withContext(Dispatchers.Default) {
                        try {
                            extractTextViaOcr(context, uri)
                        } catch (e: Exception) {
                            Log.e("ATS_FLOW", "ML Kit OCR failed on home screen", e)
                            ""
                        }
                    }
                    if (ocrText.length >= 50) {
                        extractedText = ocrText
                    }
                }

                if (extractedText.length > 50) {
                    scanStatusText = "AI is analyzing your resume..."
                    val aiResponse = viewModel.analyzeTextWithAi(extractedText)
                    if (aiResponse != null) {
                        atsResult = PdfAtsAnalyzer.AtsAnalysisResult(
                            score = aiResponse.score,
                            rating = aiResponse.rating,
                            layoutSuggestions = aiResponse.layoutSuggestions ?: emptyList(),
                            keywordSuggestions = aiResponse.keywordSuggestions ?: emptyList(),
                            metricSuggestions = aiResponse.metricSuggestions ?: emptyList(),
                            verbSuggestions = aiResponse.verbSuggestions ?: emptyList()
                        )
                        Log.i("ATS_FLOW", "HomeScreen ATS Compatibility Result (AI-POWERED): $atsResult")
                        isScanningFile = false
                        showScanReport = true
                        return@launch
                    }
                }

                // Fallback to local visual-based analysis
                scanStatusText = "Running local ATS scan..."
                val result = PdfAtsAnalyzer.analyze(context, uri, name)
                atsResult = result
                Log.i("ATS_FLOW", "HomeScreen ATS Compatibility Result (LOCAL FALLBACK): $atsResult")
                isScanningFile = false
                showScanReport = true
            }
        }
    }



    Scaffold(
        bottomBar = {
            AppBottomNavigationBar(
                navController = navController,
                currentRoute = Screen.Home.route
            )
        },
        floatingActionButton = {
            val showFab = when (val state = uiState) {
                is HomeUiState.Success -> state.documents.isNotEmpty()
                else -> false
            }
            if (showFab) {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate(Screen.ProfileEditor.route) },
                    icon = { Icon(Icons.Default.Build, contentDescription = null) },
                    text = { Text("Create Tailored Resume") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(bottom = 80.dp) // Lift FAB above bottom navigation bar
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                // 1. Top Greeting Bar
                GreetingHeader(
                    profile = userProfile,
                    onLogoutClick = {
                        viewModel.logout {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 1.5 ATS Dashboard Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "AI ATS Compatibility Analyzer",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Ensure your resume bypasses Applicant Tracking System algorithms by matching structure and keywords.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { navController.navigate(Screen.ResumePreview.route) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Analyze Profile", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            OutlinedButton(
                                onClick = { filePickerLauncher.launch("application/pdf") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Upload File", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                MainResumeCard(
                    userProfile = userProfile,
                    onEditClick = { navController.navigate(Screen.ProfileEditor.route) },
                    onDownloadClick = {
                        viewModel.downloadMainPdf(context) { success ->
                            if (success) {
                                android.widget.Toast.makeText(context, "LaTeX Resume saved to Downloads!", android.widget.Toast.LENGTH_LONG).show()
                            } else {
                                android.widget.Toast.makeText(context, "Failed to export LaTeX Resume", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    onResetClick = {
                        viewModel.clearResumeData {
                            android.widget.Toast.makeText(context, "Resume data cleared successfully", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Section Title
                Text(
                    text = "Recent Optimized Resumes",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // 2. Scrollable History List
                Box(modifier = Modifier.weight(1f)) {
                    when (val state = uiState) {
                        is HomeUiState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        is HomeUiState.Success -> {
                            if (state.documents.isEmpty()) {
                                EmptyStateView(onCreateClick = { navController.navigate(Screen.ProfileEditor.route) })
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(bottom = 100.dp) // Avoid overlap with bottom card
                                ) {
                                    items(state.documents) { document ->
                                        DocumentItemCard(
                                            document = document,
                                            onClick = { selectedDocument = document }
                                        )
                                    }
                                }
                            }
                        }
                        is HomeUiState.Error -> {
                            ErrorStateView(
                                message = state.message,
                                onRetry = { viewModel.loadDocuments() }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog to show detailed resume markdown content
    if (selectedDocument != null) {
        DocumentDetailsDialog(
            document = selectedDocument!!,
            onDismiss = { selectedDocument = null },
            onSave = { jobTitle, companyName, resumeMd ->
                viewModel.updateDocument(selectedDocument!!.id, jobTitle, companyName, resumeMd)
                selectedDocument = null
            },
            onDelete = {
                viewModel.deleteDocument(selectedDocument!!.id)
                selectedDocument = null
            },
            onDownload = {
                viewModel.downloadTailoredPdf(context, selectedDocument!!) { success ->
                    if (success) {
                        android.widget.Toast.makeText(context, "Tailored PDF saved to Downloads!", android.widget.Toast.LENGTH_LONG).show()
                    } else {
                        android.widget.Toast.makeText(context, "Failed to export PDF", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    // Dialog for tailoring a new resume
    if (showTailorDialog) {
        TailorResumeDialog(
            onDismiss = { showTailorDialog = false },
            onSubmit = { jobTitle, companyName, jdText ->
                viewModel.tailorNewResume(jobTitle, companyName, jdText)
                showTailorDialog = false
            }
        )
    }

    if (isScanningFile) {
        AlertDialog(
            onDismissRequest = { isScanningFile = false },
            title = { Text("Scanning File with AI", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(scanStatusText, style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {}
        )
    }

    if (showScanReport) {
        ModalBottomSheet(
            onDismissRequest = { showScanReport = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
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
                    IconButton(onClick = { showScanReport = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
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
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            drawArc(
                                color = outlineColor,
                                startAngle = 140f,
                                sweepAngle = 260f,
                                useCenter = false,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                            )
                            val currentScore = atsResult?.score ?: 85
                            drawArc(
                                color = scoreColor,
                                startAngle = 140f,
                                sweepAngle = (currentScore / 100f) * 260f,
                                useCenter = false,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
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
                    onClick = { filePickerLauncher.launch("application/pdf") },
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
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun GreetingHeader(profile: UserProfileDto?, onLogoutClick: () -> Unit) {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> "Good Morning"
        hour < 17 -> "Good Afternoon"
        else -> "Good Evening"
    }

    val displayName = if (profile != null) {
        val first = profile.firstName ?: ""
        if (first.isNotEmpty()) first else profile.username
    } else {
        "Professional Builder"
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Premium greeting card with a soft gradient
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$greeting,",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        )
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudQueue,
                                contentDescription = "Cloud Synced",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = onLogoutClick,
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Logout",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Stats removed for a clean & professional look
            }
        }
    }
}

// Unused GreetingStatItem removed

@Composable
fun DocumentItemCard(
    document: Document,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Document Icon Box
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = "Document Icon",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.jobTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = document.companyName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Status Badge
            val statusColor = when (document.status.uppercase()) {
                "OPTIMIZED" -> MaterialTheme.colorScheme.primary
                "GENERATED" -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.tertiary
            }

            val statusBgColor = when (document.status.uppercase()) {
                "OPTIMIZED" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                "GENERATED" -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
            }

            Box(
                modifier = Modifier
                    .background(statusBgColor, shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = document.status,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    color = statusColor
                )
            }
        }
    }
}

@Composable
fun EmptyStateView(onCreateClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = "No History",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Resume History",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Your tailored and optimized resumes will appear here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 24.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onCreateClick,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Build, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Tailored Resume", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ErrorStateView(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Error Icon",
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Oops! Something went wrong",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 24.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Try Again")
        }
    }
}

@Composable
fun TailorResumeDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String, String) -> Unit
) {
    var jobTitle by remember { mutableStateOf("") }
    var companyName by remember { mutableStateOf("") }
    var jdText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Tailor New Resume",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = jobTitle,
                    onValueChange = { jobTitle = it },
                    label = { Text("Job Title") },
                    placeholder = { Text("e.g. Android Engineer") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = { Text("Company Name") },
                    placeholder = { Text("e.g. Google") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = jdText,
                    onValueChange = { jdText = it },
                    label = { Text("Job Description") },
                    placeholder = { Text("Paste requirements here...") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (jobTitle.isNotBlank() && companyName.isNotBlank() && jdText.isNotBlank()) {
                        onSubmit(jobTitle, companyName, jdText)
                    }
                },
                enabled = jobTitle.isNotBlank() && companyName.isNotBlank() && jdText.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Optimize")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun MainResumeCard(
    userProfile: UserProfileDto?,
    onEditClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onResetClick: () -> Unit
) {
    var showResetConfirm by remember { mutableStateOf(false) }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset Resume Data?", fontWeight = FontWeight.Bold) },
            text = { Text("This will wipe all your personal details, experiences, and layout settings from local storage and the server. Are you sure?") },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirm = false
                        onResetClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "My Main LaTeX Resume",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (userProfile?.name.isNullOrBlank()) "No resume created yet" else "Master profile compiled",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onDownloadClick,
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Download PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onEditClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit Details", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = { showResetConfirm = true },
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Reset Profile",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun DocumentDetailsDialog(
    document: Document,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
    onDelete: () -> Unit,
    onDownload: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedJobTitle by remember { mutableStateOf(document.jobTitle) }
    var editedCompanyName by remember { mutableStateOf(document.companyName) }
    var editedResumeMd by remember { mutableStateOf(document.resumeMd) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Resume?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this tailored resume? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                if (isEditing) {
                    Text("Edit Tailored Resume Details", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                } else {
                    Text(
                        text = document.jobTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = document.companyName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isEditing) {
                    OutlinedTextField(
                        value = editedJobTitle,
                        onValueChange = { editedJobTitle = it },
                        label = { Text("Job Title") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editedCompanyName,
                        onValueChange = { editedCompanyName = it },
                        label = { Text("Company Name") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editedResumeMd,
                        onValueChange = { editedResumeMd = it },
                        label = { Text("Resume Markdown") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(200.dp)
                    )
                } else {
                    Column {
                        Text(
                            text = "Job Description Match Details",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = document.jdText.ifBlank { "No Job Description supplied." },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                    Column {
                        Text(
                            text = "Optimized Markdown",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                text = document.resumeMd,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isEditing) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                isEditing = false
                                editedJobTitle = document.jobTitle
                                editedCompanyName = document.companyName
                                editedResumeMd = document.resumeMd
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                isEditing = false
                                onSave(editedJobTitle, editedCompanyName, editedResumeMd)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save")
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onDownload,
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Download", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { isEditing = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Close")
                    }
                }
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

private fun extractTextFromPdfBytes(bytes: ByteArray): String {
    val content = String(bytes, Charsets.ISO_8859_1)
    val sb = StringBuilder()
    
    var index = 0
    val textBlocks = mutableListOf<String>()
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
                // Ignore
            }
        }
        
        index = endStreamIdx + 9
    }
    
    val tjPattern = Regex("""(?:\(([^)]*)\)|<([0-9a-fA-F\s]+)>)\s*(?:Tj|'|")""")
    val tjArrayPattern = Regex("""\[([^]]*)\]\s*TJ""")
    
    for (block in textBlocks) {
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
    
    return sb.toString()
        .replace(Regex("[^\\x20-\\x7E\\n\\r]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
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
        // Ignore
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
        val scale = 3.0f
        val bmpWidth = (page.width * scale).toInt()
        val bmpHeight = (page.height * scale).toInt()
        val bmp = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        canvas.drawColor(android.graphics.Color.WHITE)
        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        
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

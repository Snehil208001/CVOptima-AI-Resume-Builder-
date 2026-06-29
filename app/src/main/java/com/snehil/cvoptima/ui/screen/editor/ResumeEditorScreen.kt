package com.snehil.cvoptima.ui.screen.editor

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.activity.compose.BackHandler
import com.snehil.cvoptima.core.navigation.Screen
import com.snehil.cvoptima.data.local.entity.*
import com.snehil.cvoptima.ui.components.AppBottomNavigationBar
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.focus.FocusDirection

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ResumeEditorScreen(
    navController: NavController,
    viewModel: ResumeEditorViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(1) }
    val syncState by viewModel.syncState.collectAsState()

    // Auto-save draft when navigating away or exiting the screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.saveDraft()
        }
    }

    // Intercept system back press to save draft
    BackHandler(enabled = true) {
        viewModel.saveDraft()
        navController.popBackStack()
    }

    // Dialog state management
    var showEduDialog by remember { mutableStateOf(false) }
    var editingEdu by remember { mutableStateOf<LocalEducation?>(null) }

    var showExpDialog by remember { mutableStateOf(false) }
    var editingExp by remember { mutableStateOf<LocalExperience?>(null) }

    var showProjDialog by remember { mutableStateOf(false) }
    var editingProj by remember { mutableStateOf<LocalProject?>(null) }

    var showCertDialog by remember { mutableStateOf(false) }
    var editingCert by remember { mutableStateOf<LocalCertification?>(null) }

    var showAddSkillGroupDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Resume Onboarding",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                actions = {
                    IconButton(onClick = { viewModel.loadData(forceRefresh = true) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Data")
                    }
                }
            )
        },
        bottomBar = {
            AppBottomNavigationBar(
                navController = navController,
                currentRoute = Screen.ProfileEditor.route
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Steps Indicator
                StepProgressBar(currentStep = currentStep)

                Spacer(modifier = Modifier.height(12.dp))

                // Active Step Content Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (currentStep) {
                        1 -> Step1BasicInfo(viewModel = viewModel)
                        2 -> Step2Lists(
                            viewModel = viewModel,
                            onAddEdu = {
                                editingEdu = null
                                showEduDialog = true
                            },
                            onEditEdu = {
                                editingEdu = it
                                showEduDialog = true
                            },
                            onAddExp = {
                                editingExp = null
                                showExpDialog = true
                            },
                            onEditExp = {
                                editingExp = it
                                showExpDialog = true
                            },
                            onAddProj = {
                                editingProj = null
                                showProjDialog = true
                            },
                            onEditProj = {
                                editingProj = it
                                showProjDialog = true
                            },
                            onAddCert = {
                                editingCert = null
                                showCertDialog = true
                            },
                            onEditCert = {
                                editingCert = it
                                showCertDialog = true
                            }
                        )
                        3 -> Step3Skills(
                            viewModel = viewModel,
                            onAddGroup = { showAddSkillGroupDialog = true }
                        )
                        4 -> Step4Layout(viewModel = viewModel)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Navigation controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { 
                            viewModel.saveDraft()
                            if (currentStep > 1) currentStep-- 
                        },
                        enabled = currentStep > 1,
                        colors = ButtonDefaults.filledTonalButtonColors()
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Back")
                    }

                    if (currentStep < 4) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.saveAndSync { success, message ->
                                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                    }
                                },
                                enabled = syncState !is SyncState.Syncing,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (syncState is SyncState.Syncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Saving...")
                                } else {
                                    Icon(Icons.Default.Save, contentDescription = "Save Progress", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Save")
                                }
                            }

                            Button(
                                onClick = {
                                    viewModel.saveDraft()
                                    if (currentStep == 1) {
                                        val err = viewModel.validateInputs()
                                        if (err != null) {
                                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                        } else {
                                            currentStep++
                                        }
                                    } else {
                                        currentStep++
                                    }
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Next")
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowForward, contentDescription = "Next")
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                viewModel.saveAndSync { success, message ->
                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                     if (success) {
                                         navController.navigate(Screen.ResumePreview.route) {
                                             popUpTo(Screen.Home.route)
                                         }
                                     }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            enabled = syncState !is SyncState.Syncing,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (syncState is SyncState.Syncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Syncing...")
                            } else {
                                Icon(Icons.Default.CloudUpload, contentDescription = "Compile & Sync")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Compile & Sync")
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Dialogs Definitions ---

    // 1. Education Dialog
    if (showEduDialog) {
        EducationAddEditDialog(
            edu = editingEdu,
            onDismiss = { showEduDialog = false },
            onSave = { newEdu ->
                if (editingEdu != null) {
                    val index = viewModel.educations.indexOf(editingEdu)
                    if (index >= 0) viewModel.educations[index] = newEdu
                } else {
                    viewModel.educations.add(newEdu)
                }
                viewModel.saveDraft()
                showEduDialog = false
            }
        )
    }

    // 2. Experience Dialog
    if (showExpDialog) {
        ExperienceAddEditDialog(
            exp = editingExp,
            onDismiss = { showExpDialog = false },
            onSave = { newExp ->
                if (editingExp != null) {
                    val index = viewModel.experiences.indexOf(editingExp)
                    if (index >= 0) viewModel.experiences[index] = newExp
                } else {
                    viewModel.experiences.add(newExp)
                }
                viewModel.saveDraft()
                showExpDialog = false
            }
        )
    }

    // 3. Project Dialog
    if (showProjDialog) {
        ProjectAddEditDialog(
            proj = editingProj,
            onDismiss = { showProjDialog = false },
            onSave = { newProj ->
                if (editingProj != null) {
                    val index = viewModel.projects.indexOf(editingProj)
                    if (index >= 0) viewModel.projects[index] = newProj
                } else {
                    viewModel.projects.add(newProj)
                }
                viewModel.saveDraft()
                showProjDialog = false
            }
        )
    }

    // 4. Certification Dialog
    if (showCertDialog) {
        CertificationAddEditDialog(
            cert = editingCert,
            onDismiss = { showCertDialog = false },
            onSave = { newCert ->
                if (editingCert != null) {
                    val index = viewModel.certifications.indexOf(editingCert)
                    if (index >= 0) viewModel.certifications[index] = newCert
                } else {
                    viewModel.certifications.add(newCert)
                }
                viewModel.saveDraft()
                showCertDialog = false
            }
        )
    }

    // 5. Add Skill Group Dialog
    if (showAddSkillGroupDialog) {
        var groupLabel by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddSkillGroupDialog = false },
            title = { Text("Add Skill Group") },
            text = {
                OutlinedTextField(
                    value = groupLabel,
                    onValueChange = { groupLabel = it },
                    label = { Text("Group Label (e.g. Languages)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (groupLabel.isNotBlank()) {
                            viewModel.skillGroups.add(
                                LocalSkillGroup(
                                    label = groupLabel,
                                    skills = emptyList(),
                                    resumeId = 1L
                                )
                            )
                            viewModel.saveDraft()
                        }
                        showAddSkillGroupDialog = false
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSkillGroupDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ================= STEP INDICATOR =================

@Composable
fun StepProgressBar(currentStep: Int) {
    val steps = listOf("Basic Info", "Core History", "Skills System", "Layout Setup")
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.forEachIndexed { index, name ->
                val stepNum = index + 1
                val isActive = stepNum <= currentStep
                val isCurrent = stepNum == currentStep

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCurrent) MaterialTheme.colorScheme.primary
                                else if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            )
                            .border(
                                width = 2.dp,
                                color = if (isCurrent) MaterialTheme.colorScheme.outline else Color.Transparent,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isActive && !isCurrent) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        } else {
                            Text(
                                stepNum.toString(),
                                color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        name,
                        fontSize = 10.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { (currentStep - 1) / 3f },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        )
    }
}

// ================= STEP 1: BASIC INFO =================

@Composable
fun Step1BasicInfo(viewModel: ResumeEditorViewModel) {
    val focusRequesterContact = remember { FocusRequester() }
    val focusRequesterLocation = remember { FocusRequester() }
    val focusRequesterYears = remember { FocusRequester() }
    val focusRequesterSpec = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Personal Details",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = viewModel.name,
                    onValueChange = { viewModel.name = it },
                    label = { Text("Full Name *") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = viewModel.email,
                    onValueChange = { viewModel.email = it },
                    label = { Text("Email Address *") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = viewModel.contactNumber,
                        onValueChange = { viewModel.contactNumber = it },
                        label = { Text("Contact *") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequesterContact),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next,
                            keyboardType = KeyboardType.Phone
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusRequesterLocation.requestFocus() }
                        )
                    )

                    OutlinedTextField(
                        value = viewModel.location,
                        onValueChange = { viewModel.location = it },
                        label = { Text("Location *") },
                        leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequesterLocation),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )
                }
            }
        }

        Text(
            "Links & Socials",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = viewModel.linkedinUrl,
                    onValueChange = { viewModel.linkedinUrl = it },
                    label = { Text("LinkedIn URL") },
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = viewModel.githubUrl,
                    onValueChange = { viewModel.githubUrl = it },
                    label = { Text("GitHub URL") },
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = viewModel.portfolioUrl,
                    onValueChange = { viewModel.portfolioUrl = it },
                    label = { Text("Portfolio URL") },
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Text(
            "AI Summary Assistant",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = viewModel.yearsOfExp,
                        onValueChange = { viewModel.yearsOfExp = it },
                        label = { Text("Experience") },
                        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequesterYears),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusRequesterSpec.requestFocus() }
                        )
                    )
                    OutlinedTextField(
                        value = viewModel.specialization,
                        onValueChange = { viewModel.specialization = it },
                        label = { Text("Specialization") },
                        leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequesterSpec),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )
                }
                OutlinedTextField(
                    value = viewModel.targetRole,
                    onValueChange = { viewModel.targetRole = it },
                    label = { Text("Seeking Role (e.g., Senior Android Developer)") },
                    leadingIcon = { Icon(Icons.Default.Work, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = viewModel.primaryTechnologies,
                    onValueChange = { viewModel.primaryTechnologies = it },
                    label = { Text("Primary Technologies (e.g., Kotlin, Java, SQL)") },
                    leadingIcon = { Icon(Icons.Default.Build, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    viewModel.autoGenerateSummary = !viewModel.autoGenerateSummary
                    if (viewModel.autoGenerateSummary) {
                        viewModel.generateAiSummary()
                    }
                }
                .padding(vertical = 8.dp)
        ) {
            Checkbox(
                checked = viewModel.autoGenerateSummary,
                onCheckedChange = {
                    viewModel.autoGenerateSummary = it
                    if (it) {
                        viewModel.generateAiSummary()
                    }
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Auto-Generate with AI Professional Summary")
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = viewModel.professionalSummary,
                    onValueChange = { viewModel.professionalSummary = it },
                    label = { Text("Professional Summary") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ================= STEP 2: TABBED LISTS =================

@Composable
fun Step2Lists(
    viewModel: ResumeEditorViewModel,
    onAddEdu: () -> Unit,
    onEditEdu: (LocalEducation) -> Unit,
    onAddExp: () -> Unit,
    onEditExp: (LocalExperience) -> Unit,
    onAddProj: () -> Unit,
    onEditProj: (LocalProject) -> Unit,
    onAddCert: () -> Unit,
    onEditCert: (LocalCertification) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Work Exp", "Education", "Projects", "Certs")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> Step2Experiences(viewModel, onAddExp, onEditExp)
                1 -> Step2Educations(viewModel, onAddEdu, onEditEdu)
                2 -> Step2Projects(viewModel, onAddProj, onEditProj)
                3 -> Step2Certifications(viewModel, onAddCert, onEditCert)
            }
        }
    }
}

@Composable
fun Step2Experiences(
    viewModel: ResumeEditorViewModel,
    onAdd: () -> Unit,
    onEdit: (LocalExperience) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (viewModel.experiences.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No experience entries added. Tap + to add.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viewModel.experiences) { exp ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(exp.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("${exp.company} • ${exp.location ?: "Unknown"}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${exp.startDate ?: ""} - ${if (exp.isCurrentRole) "Present" else exp.endDate ?: ""}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row {
                                IconButton(onClick = { onEdit(exp) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { viewModel.experiences.remove(exp); viewModel.saveDraft() }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        FloatingActionButton(
            onClick = onAdd,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Experience", tint = Color.White)
        }
    }
}

@Composable
fun Step2Educations(
    viewModel: ResumeEditorViewModel,
    onAdd: () -> Unit,
    onEdit: (LocalEducation) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (viewModel.educations.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No education entries added. Tap + to add.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viewModel.educations) { edu ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(edu.degree, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("${edu.institution} • ${edu.location ?: "Unknown"}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Score: ${edu.score ?: edu.gpa?.toString() ?: "N/A"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row {
                                IconButton(onClick = { onEdit(edu) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { viewModel.educations.remove(edu); viewModel.saveDraft() }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        FloatingActionButton(
            onClick = onAdd,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Education", tint = Color.White)
        }
    }
}

@Composable
fun Step2Projects(
    viewModel: ResumeEditorViewModel,
    onAdd: () -> Unit,
    onEdit: (LocalProject) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (viewModel.projects.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No projects added. Tap + to add.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viewModel.projects) { proj ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(proj.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                if (!proj.techStack.isNullOrBlank()) {
                                    Text("Tech Stack: ${proj.techStack}", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                if (!proj.date.isNullOrBlank()) {
                                    Text("Date: ${proj.date}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Row {
                                IconButton(onClick = { onEdit(proj) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { viewModel.projects.remove(proj); viewModel.saveDraft() }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        FloatingActionButton(
            onClick = onAdd,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Project", tint = Color.White)
        }
    }
}

@Composable
fun Step2Certifications(
    viewModel: ResumeEditorViewModel,
    onAdd: () -> Unit,
    onEdit: (LocalCertification) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (viewModel.certifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No certifications or achievements added. Tap + to add.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viewModel.certifications) { cert ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(cert.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                if (!cert.issuer.isNullOrBlank()) {
                                    Text("Issuer: ${cert.issuer}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (!cert.date.isNullOrBlank()) {
                                    Text("Date: ${cert.date}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Row {
                                IconButton(onClick = { onEdit(cert) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { viewModel.certifications.remove(cert); viewModel.saveDraft() }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        FloatingActionButton(
            onClick = onAdd,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Certification", tint = Color.White)
        }
    }
}

// ================= STEP 3: SKILLS SYSTEM =================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Step3Skills(
    viewModel: ResumeEditorViewModel,
    onAddGroup: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Categorized Skills", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Button(onClick = onAddGroup, shape = RoundedCornerShape(8.dp)) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Category")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (viewModel.skillGroups.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No skill categories. Tap 'Add Category' to start.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(viewModel.skillGroups) { group ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Category Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    group.label,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(onClick = { viewModel.skillGroups.remove(group) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Group", tint = MaterialTheme.colorScheme.error)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Tag list layout using FlowRow
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                group.skills.forEach { tag ->
                                    InputChip(
                                        selected = true,
                                        onClick = {},
                                        label = { Text(tag) },
                                        trailingIcon = {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Remove Tag",
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clickable {
                                                        val updatedSkills = group.skills.toMutableList().apply { remove(tag) }
                                                        val index = viewModel.skillGroups.indexOf(group)
                                                        if (index >= 0) {
                                                            viewModel.skillGroups[index] = group.copy(skills = updatedSkills)
                                                        }
                                                    }
                                            )
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Inline tags builder
                            var newSkillText by remember { mutableStateOf("") }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newSkillText,
                                    onValueChange = { newSkillText = it },
                                    label = { Text("Add Skill Tag (e.g. Kotlin)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        if (newSkillText.isNotBlank()) {
                                            val updatedSkills = group.skills.toMutableList().apply { add(newSkillText.trim()) }
                                            val index = viewModel.skillGroups.indexOf(group)
                                            if (index >= 0) {
                                                viewModel.skillGroups[index] = group.copy(skills = updatedSkills)
                                            }
                                            newSkillText = ""
                                        }
                                    },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Skill", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ================= STEP 4: LAYOUT CONFIG =================

@Composable
fun Step4Layout(viewModel: ResumeEditorViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Layout Density", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val densities = listOf("Compact", "Normal", "Spacious")
            densities.forEach { density ->
                val isSelected = viewModel.layoutDensity == density
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.layoutDensity = density },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = if (isSelected) borderStrokeForDensity() else null
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(density, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Section Arrangement (Reorder)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(viewModel.sectionOrder) { section ->
                val index = viewModel.sectionOrder.indexOf(section)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DragHandle, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(section, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                        }

                        Row {
                            IconButton(
                                onClick = {
                                    if (index > 0) {
                                        viewModel.sectionOrder.removeAt(index)
                                        viewModel.sectionOrder.add(index - 1, section)
                                    }
                                },
                                enabled = index > 0
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up")
                            }

                            IconButton(
                                onClick = {
                                    if (index < viewModel.sectionOrder.size - 1) {
                                        viewModel.sectionOrder.removeAt(index)
                                        viewModel.sectionOrder.add(index + 1, section)
                                    }
                                },
                                enabled = index < viewModel.sectionOrder.size - 1
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun borderStrokeForDensity(): androidx.compose.foundation.BorderStroke {
    return androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
}

// ================= DIALOG IMPLEMENTATIONS =================

@Composable
fun EducationAddEditDialog(
    edu: LocalEducation?,
    onDismiss: () -> Unit,
    onSave: (LocalEducation) -> Unit
) {
    var institution by remember { mutableStateOf(edu?.institution ?: "") }
    var degree by remember { mutableStateOf(edu?.degree ?: "") }
    var fieldOfStudy by remember { mutableStateOf(edu?.fieldOfStudy ?: "") }
    var startDate by remember { mutableStateOf(edu?.startDate ?: "") }
    var endDate by remember { mutableStateOf(edu?.endDate ?: "") }
    var score by remember { mutableStateOf(edu?.score ?: edu?.gpa?.toString() ?: "") }
    var location by remember { mutableStateOf(edu?.location ?: "") }

    val focusRequester1 = remember { FocusRequester() }
    val focusRequester2 = remember { FocusRequester() }
    val focusRequester3 = remember { FocusRequester() }
    val focusRequester4 = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (edu != null) "Edit Education" else "Add Education") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = institution, onValueChange = { institution = it }, label = { Text("Institution *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = degree, onValueChange = { degree = it }, label = { Text("Degree *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = fieldOfStudy, onValueChange = { fieldOfStudy = it }, label = { Text("Field of Study") }, modifier = Modifier.fillMaxWidth())
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = { startDate = it },
                        label = { Text("Start Date") },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester1),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusRequester2.requestFocus() })
                    )
                    OutlinedTextField(
                        value = endDate,
                        onValueChange = { endDate = it },
                        label = { Text("End Date") },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester2),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusRequester3.requestFocus() })
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = score,
                        onValueChange = { score = it },
                        label = { Text("GPA/Score") },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester3),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusRequester4.requestFocus() })
                    )
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location") },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester4),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (institution.isNotBlank() && degree.isNotBlank()) {
                        val parsedGpa = score.toDoubleOrNull()
                        onSave(
                            LocalEducation(
                                id = edu?.id ?: 0,
                                institution = institution,
                                degree = degree,
                                fieldOfStudy = fieldOfStudy,
                                startDate = startDate,
                                endDate = endDate,
                                gpa = parsedGpa,
                                score = score,
                                location = location,
                                resumeId = 1L
                            )
                        )
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ExperienceAddEditDialog(
    exp: LocalExperience?,
    onDismiss: () -> Unit,
    onSave: (LocalExperience) -> Unit
) {
    var company by remember { mutableStateOf(exp?.company ?: "") }
    var title by remember { mutableStateOf(exp?.title ?: "") }
    var startDate by remember { mutableStateOf(exp?.startDate ?: "") }
    var endDate by remember { mutableStateOf(exp?.endDate ?: "") }
    var isCurrentRole by remember { mutableStateOf(exp?.isCurrentRole ?: false) }
    var location by remember { mutableStateOf(exp?.location ?: "") }
    var type by remember { mutableStateOf(exp?.type ?: "Onsite") } // e.g. Remote, Hybrid, Onsite
    val bulletPoints = remember { mutableStateListOf<String>().apply { addAll(exp?.bulletPoints ?: emptyList()) } }

    val focusRequesterStart = remember { FocusRequester() }
    val focusRequesterEnd = remember { FocusRequester() }
    val focusRequesterLocation = remember { FocusRequester() }
    val focusRequesterType = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (exp != null) "Edit Experience" else "Add Experience") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = company, onValueChange = { company = it }, label = { Text("Company *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title/Role *") }, modifier = Modifier.fillMaxWidth())
                if (isCurrentRole) {
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = { startDate = it },
                        label = { Text("Start Date *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequesterStart),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusRequesterLocation.requestFocus() })
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = startDate,
                            onValueChange = { startDate = it },
                            label = { Text("Start Date *") },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequesterStart),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusRequesterEnd.requestFocus() })
                        )
                        OutlinedTextField(
                            value = endDate,
                            onValueChange = { endDate = it },
                            label = { Text("End Date") },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequesterEnd),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusRequesterLocation.requestFocus() })
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isCurrentRole, onCheckedChange = { isCurrentRole = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("I currently work in this role")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location") },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequesterLocation),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusRequesterType.requestFocus() })
                    )
                    OutlinedTextField(
                        value = type,
                        onValueChange = { type = it },
                        label = { Text("Type") },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequesterType),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Bullet Description Points", fontWeight = FontWeight.Bold)

                bulletPoints.forEachIndexed { i, bullet ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = bullet,
                            onValueChange = { bulletPoints[i] = it },
                            label = { Text("Bullet ${i+1}") },
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { bulletPoints.removeAt(i) }) {
                            Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove bullet")
                        }
                    }
                }

                TextButton(
                    onClick = { bulletPoints.add("") }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Bullet Point")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (company.isNotBlank() && title.isNotBlank()) {
                        onSave(
                            LocalExperience(
                                id = exp?.id ?: 0,
                                company = company,
                                title = title,
                                startDate = startDate,
                                endDate = if (isCurrentRole) "Present" else endDate,
                                isCurrentRole = isCurrentRole,
                                location = location,
                                type = type,
                                description = bulletPoints.firstOrNull() ?: "",
                                bulletPoints = bulletPoints.filter { it.isNotBlank() },
                                resumeId = 1L
                            )
                        )
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ProjectAddEditDialog(
    proj: LocalProject?,
    onDismiss: () -> Unit,
    onSave: (LocalProject) -> Unit
) {
    var title by remember { mutableStateOf(proj?.title ?: "") }
    var link by remember { mutableStateOf(proj?.link ?: "") }
    var date by remember { mutableStateOf(proj?.date ?: "") }
    var techStack by remember { mutableStateOf(proj?.techStack ?: "") }
    val bulletPoints = remember { mutableStateListOf<String>().apply { addAll(proj?.bulletPoints ?: emptyList()) } }

    val focusRequesterDate = remember { FocusRequester() }
    val focusRequesterTech = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (proj != null) "Edit Project" else "Add Project") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Project Title *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = link, onValueChange = { link = it }, label = { Text("Project Link") }, modifier = Modifier.fillMaxWidth())
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Timeline") },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequesterDate),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusRequesterTech.requestFocus() })
                    )
                    OutlinedTextField(
                        value = techStack,
                        onValueChange = { techStack = it },
                        label = { Text("Technologies") },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequesterTech),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Bullet Project Details", fontWeight = FontWeight.Bold)

                bulletPoints.forEachIndexed { i, bullet ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = bullet,
                            onValueChange = { bulletPoints[i] = it },
                            label = { Text("Bullet ${i+1}") },
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { bulletPoints.removeAt(i) }) {
                            Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove bullet")
                        }
                    }
                }

                TextButton(
                    onClick = { bulletPoints.add("") }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Bullet Point")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            LocalProject(
                                id = proj?.id ?: 0,
                                title = title,
                                link = link,
                                date = date,
                                techStack = techStack,
                                bulletPoints = bulletPoints.filter { it.isNotBlank() },
                                resumeId = 1L
                            )
                        )
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CertificationAddEditDialog(
    cert: LocalCertification?,
    onDismiss: () -> Unit,
    onSave: (LocalCertification) -> Unit
) {
    var title by remember { mutableStateOf(cert?.title ?: "") }
    var issuer by remember { mutableStateOf(cert?.issuer ?: "") }
    var link by remember { mutableStateOf(cert?.link ?: "") }
    var date by remember { mutableStateOf(cert?.date ?: "") }
    val bulletPoints = remember { mutableStateListOf<String>().apply { addAll(cert?.bulletPoints ?: emptyList()) } }

    val focusRequesterLink = remember { FocusRequester() }
    val focusRequesterDate = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (cert != null) "Edit Certification/Achievement" else "Add Certification/Achievement") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = issuer, onValueChange = { issuer = it }, label = { Text("Issuer") }, modifier = Modifier.fillMaxWidth())
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = link,
                        onValueChange = { link = it },
                        label = { Text("Link") },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequesterLink),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusRequesterDate.requestFocus() })
                    )
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date/Year") },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequesterDate),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Bullet Details", fontWeight = FontWeight.Bold)

                bulletPoints.forEachIndexed { i, bullet ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = bullet,
                            onValueChange = { bulletPoints[i] = it },
                            label = { Text("Bullet ${i+1}") },
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { bulletPoints.removeAt(i) }) {
                            Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove bullet")
                        }
                    }
                }

                TextButton(
                    onClick = { bulletPoints.add("") }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Bullet Point")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            LocalCertification(
                                id = cert?.id ?: 0,
                                title = title,
                                issuer = issuer,
                                link = link,
                                date = date,
                                bulletPoints = bulletPoints.filter { it.isNotBlank() },
                                resumeId = 1L
                            )
                        )
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

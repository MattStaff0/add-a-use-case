<template>
  <el-form
    ref="studentForm"
    :model="studentBeingEdited"
    :rules="rules"
    label-width="110px"
    style="padding-right: 30px"
    label-position="left"
  >
    <el-form-item label="Id:">
      <el-input v-model="studentBeingEdited.id" disabled></el-input>
    </el-form-item>
    <el-form-item label="Username:" prop="username">
      <el-input v-model="studentBeingEdited.username"></el-input>
    </el-form-item>
    <el-form-item label="Status:">
      <el-tag v-if="studentBeingEdited.enabled" type="success">Active</el-tag>
      <el-tag v-else type="danger">Deactivated</el-tag>
      <el-button
        v-if="studentBeingEdited.enabled"
        type="danger"
        plain
        size="small"
        style="margin-left: 12px"
        :loading="deactivating"
        @click="deactivateExistingStudent()"
      >
        Deactivate
      </el-button>
    </el-form-item>
    <el-form-item label="First Name:" prop="firstName">
      <el-input v-model="studentBeingEdited.firstName"></el-input>
    </el-form-item>
    <el-form-item label="Last Name:" prop="lastName">
      <el-input v-model="studentBeingEdited.lastName"></el-input>
    </el-form-item>
    <el-form-item label="Email:" prop="email">
      <el-input v-model="studentBeingEdited.email"></el-input>
    </el-form-item>
    <el-form-item label="Team:" prop="teamName">
      <el-input v-model="studentBeingEdited.teamName" disabled></el-input>
    </el-form-item>
    <div style="display: flex; justify-content: end">
      <el-button @click="$emit('close-dialog')"> Cancel </el-button>
      <el-button type="primary" @click="updateExistingStudent()"> Confirm </el-button>
    </div>
  </el-form>
</template>

<script setup lang="ts">
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Student } from '@/apis/student/types'
import type { FormInstance } from 'element-plus'
import { deactivateStudent, findStudentById, updateStudent } from '@/apis/student'
import { h, ref, onMounted } from 'vue'

const { studentId } = defineProps(['studentId'])
const emit = defineEmits(['close-dialog'])

// Note that the structure of the userData is NOT the same as the User interface since the roles are stored as an array
const studentBeingEdited = ref<Student>({
  // Data of the user being added or edited
  id: NaN,
  username: '',
  enabled: false,
  firstName: '',
  lastName: '',
  email: ''
})

const studentForm = ref<FormInstance>() // Form reference

// Validation rules
const rules = {
  username: [
    { required: true, message: 'Please input your email as your username', trigger: 'blur' },
    {
      pattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
      message: 'Enter your email as username.',
      trigger: 'blur'
    }
  ],
  firstName: [
    { required: true, message: 'Please provide the first name of the user.', trigger: 'blur' }
  ],
  lastName: [
    { required: true, message: 'Please provide the last name of the user.', trigger: 'blur' }
  ],
  email: [
    { required: true, message: 'Please provide the email of the user.', trigger: 'blur' },
    { type: 'email', message: 'Please provide a valid email.', trigger: 'blur' }
  ]
}

async function loadStudent(studentId: number) {
  // Load the user to be edited
  const result = await findStudentById(studentId)

  studentBeingEdited.value.id = result.data.id as number
  studentBeingEdited.value.username = result.data.username
  studentBeingEdited.value.enabled = result.data.enabled
  studentBeingEdited.value.firstName = result.data.firstName
  studentBeingEdited.value.lastName = result.data.lastName
  studentBeingEdited.value.email = result.data.email
  studentBeingEdited.value.teamName = result.data.teamName
}

onMounted(() => {
  loadStudent(studentId) // Load the artifact to be edited
})

const deactivating = ref(false)

// What deactivation does, shown before the instructor confirms (UC-STU-deactivate-student step 4)
const deactivationConsequences = [
  'They will no longer be able to log in or submit weekly activity reports and peer evaluations. If they are logged in now, their session stops working at its next request.',
  'They stay on their team. Removing a student from a team is a separate course admin action.',
  'Their teammates no longer evaluate them and are no longer asked to. The peer evaluation report lists them only for weeks in which they received a peer evaluation.',
  'They no longer owe weekly activity reports or peer evaluations and receive no reminders. A teammate who did not evaluate them in a past week no longer owes that evaluation.',
  'Their account and submitted work are kept. This is not a deletion, and the account can be reactivated later.'
]

// Deactivating neither saves nor discards the other fields of this form
async function deactivateExistingStudent() {
  const name = `${studentBeingEdited.value.firstName} ${studentBeingEdited.value.lastName}`
  try {
    await ElMessageBox.confirm(
      h('div', [
        h('p', `Deactivate ${name}?`),
        h(
          'ul',
          { style: 'padding-left: 18px' },
          deactivationConsequences.map((consequence) => h('li', consequence))
        )
      ]),
      'Deactivate Student',
      { confirmButtonText: 'Deactivate', cancelButtonText: 'Cancel', type: 'warning' }
    )
  } catch {
    return // Cancelled (5a): nothing is sent
  }

  deactivating.value = true
  try {
    const result = await deactivateStudent(studentBeingEdited.value.id as number)
    studentBeingEdited.value.enabled = result.data.enabled
    ElMessage.success(`${name} has been deactivated.`)
  } catch (error) {
    // Already deactivated by someone else (6a): request.ts has shown the message, so only the status is refreshed
    if (axios.isAxiosError(error) && error.response?.status === 409) {
      const result = await findStudentById(studentBeingEdited.value.id as number)
      studentBeingEdited.value.enabled = result.data.enabled
    }
  } finally {
    deactivating.value = false
  }
}

async function updateExistingStudent() {
  await studentForm.value!.validate() // Validate the form

  const updatedStudent: Student = {
    id: studentBeingEdited.value.id,
    username: studentBeingEdited.value.username,
    enabled: studentBeingEdited.value.enabled,
    firstName: studentBeingEdited.value.firstName,
    lastName: studentBeingEdited.value.lastName,
    email: studentBeingEdited.value.email
  }
  console.log(updatedStudent)
  // Call the API to update the user
  await updateStudent(updatedStudent)

  ElMessage.success('Student updated successfully')
  emit('close-dialog')
}
</script>

<style scoped lang="scss"></style>

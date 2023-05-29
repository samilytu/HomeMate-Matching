package com.samilozturk.homemate_matching.data.source.db.impl

import com.google.android.gms.maps.model.LatLng
import com.samilozturk.homemate_matching.data.model.MatchRequest
import com.samilozturk.homemate_matching.data.model.MatchRequest.Matched
import com.samilozturk.homemate_matching.data.model.MatchRequest.Received
import com.samilozturk.homemate_matching.data.model.MatchRequest.Sent
import com.samilozturk.homemate_matching.data.model.Student
import com.samilozturk.homemate_matching.data.model.Student.MatchingStatus.NoRequest
import com.samilozturk.homemate_matching.data.model.Student.MatchingStatus.MatchedRequest
import com.samilozturk.homemate_matching.data.model.Student.MatchingStatus.ReceivedRequest
import com.samilozturk.homemate_matching.data.model.Student.MatchingStatus.SentRequest
import com.samilozturk.homemate_matching.data.model.UniqueId
import com.samilozturk.homemate_matching.data.source.auth.AuthRepository
import com.samilozturk.homemate_matching.data.source.db.DbRepository
import com.samilozturk.homemate_matching.data.source.notification.NotificationRepository
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.random.nextInt

class FakeDbRepository(
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository,
) : DbRepository {

    private val students = mutableListOf<Student>(
        Student.Factory.create(
            uid = "123",
            firstName = "Kristin",
            lastName = "Jones",
            email = "bridget.bonner@example.com",
            imageUrl = "https://picsum.photos/id/237/500/500",
            phone = null,
            department = "Bilgisayar Mühendisliği",
            grade = 3,
            homeAddress = "İstanbul Teknik Üniversitesi, Maslak, Sarıyer/İstanbul, Türkiye",
            homeLocation = LatLng(41.104444, 28.922222),
            distanceToUniversity = 5f,
            availableTime = 5,
            isProvider = true,
        ),
        Student.Factory.create(
            uid = "456",
            firstName = "Joanna",
            lastName = "Barlow",
            email = "chuck.horn@example.com",
            imageUrl = null,
            phone = null,
            department = "Elektrik Mühendisliği",
            grade = 2,
            distanceToUniversity = null,
            availableTime = null,
        ),
        Student.Factory.create(
            uid = "789",
            firstName = "Marcel",
            lastName = "Burnett",
            email = "justin.blair@example.com",
            imageUrl = "https://picsum.photos/id/244/500/500",
            phone = null,
            department = null,
            grade = null,
            distanceToUniversity = 3f,
            availableTime = 4,
            isSeeker = true,
            matchingStatus = SentRequest(UniqueId("101"))
        ),
        Student.Factory.create(
            uid = "101",
            firstName = "Alicia",
            lastName = "Harris",
            email = "marcel@burnett.com",
            imageUrl = "https://picsum.photos/id/213/500/500",
            phone = null,
            department = "Ekonomi",
            grade = 1,
            distanceToUniversity = 2f,
            availableTime = 7,
            isSeeker = true,
            matchingStatus = MatchedRequest(UniqueId("101"))
        ),
        Student.Factory.create(
            uid = "786",
            firstName = "Wade",
            lastName = "Warren",
            email = "wade.warren@example.com",
            imageUrl = "https://picsum.photos/id/237/500/500",
            phone = null,
            department = "Bilgisayar Mühendisliği",
            grade = 3,
            homeAddress = "Yıldız Teknik Üniversitesi, Davutpaşa Kampüsü, 34220 Esenler/İstanbul, Türkiye",
            homeLocation = LatLng(41.0859, 28.95181),
            distanceToUniversity = 5f,
            availableTime = 5,
            isProvider = true,
        ),
        Student.Factory.create(
            uid = "378",
            firstName = "Tara",
            lastName = "Harris",
            email = "tara.harris@example.com",
            imageUrl = "https://picsum.photos/id/252/500/500",
            phone = null,
            department = "Bilgisayar Mühendisliği",
            grade = 3,
            homeAddress = "Esenler Mahallesi, 34220 Esenler/İstanbul, Türkiye",
            homeLocation = LatLng(41.021439, 29.138081),
            distanceToUniversity = 5f,
            availableTime = 5,
            isProvider = true,
        ),
        Student.Factory.create(
            uid = "389",
            firstName = "Owen",
            lastName = "Reid",
            email = "owen.reid@example.com",
            imageUrl = null,
            phone = null,
            department = "Bilgisayar Mühendisliği",
            grade = 3,
            homeAddress = "Uğur Mumcu Mahallesi, 34349 Beşiktaş/İstanbul, Türkiye",
            homeLocation = LatLng(41.02874, 28.793044),
            distanceToUniversity = 5f,
            availableTime = 5,
            isProvider = true,
        ),
        Student.Factory.create(
            uid = "894",
            firstName = "Isaac",
            lastName = "Nelson",
            email = "isaac.nelson@example.com",
            imageUrl = "https://picsum.photos/id/297/500/500",
            phone = null,
            department = "Bilgisayar Mühendisliği",
            grade = 3,
            homeAddress = "Ihlamurkuyu Mahallesi, 34775 Ümraniye/İstanbul, Türkiye",
            homeLocation = LatLng(40.969084, 29.115026),
            distanceToUniversity = 5f,
            availableTime = 5,
            isProvider = true,
        ),
        Student.Factory.create(
            uid = "785",
            firstName = "Peyton",
            lastName = "Adams",
            email = "peyton.adams@example.com",
            imageUrl = "https://picsum.photos/id/298/500/500",
            phone = null,
            department = "Bilgisayar Mühendisliği",
            grade = 3,
            distanceToUniversity = 5f,
            availableTime = 5,
            isSeeker = true,
        ),
    )
    private val requests = mutableListOf<MatchRequest>(
        Received(
            uid = UniqueId("123"),
            targetStudent = Student.Factory.create(
                uid = "456",
                firstName = "Joanna",
                lastName = "Barlow",
                email = "chuck.horn@example.com",
                imageUrl = null,
                phone = "+90 555 555 5555",
                department = "Elektrik Mühendisliği",
                grade = 2,
                distanceToUniversity = 3f,
                availableTime = 4,
                isSeeker = true,
                matchingStatus = ReceivedRequest(UniqueId("123"))
            )
        ),
        Received(
            uid = UniqueId("456"),
            targetStudent = Student.Factory.create(
                uid = "789",
                firstName = "Marcel",
                lastName = "Burnett",
                email = "justin.blair@example.com",
                imageUrl = "https://picsum.photos/id/244/500/500",
                phone = null,
                department = null,
                grade = null,
                distanceToUniversity = 3f,
                availableTime = 4,
                isSeeker = true,
                matchingStatus = ReceivedRequest(UniqueId("456"))
            ),
        ),
        Received(
            uid = UniqueId("789"),
            targetStudent = Student.Factory.create(
                uid = "101",
                firstName = "Alicia",
                lastName = "Harris",
                email = "marcel@burnett.com",
                imageUrl = "https://picsum.photos/id/213/500/500",
                phone = null,
                department = "Ekonomi",
                grade = 1,
                distanceToUniversity = 2f,
                availableTime = 7,
                isSeeker = true,
                matchingStatus = ReceivedRequest(UniqueId("789"))
            ),
        ),
        Sent(
            uid = UniqueId("101"),
            targetStudent = Student.Factory.create(
                uid = "389",
                firstName = "Owen",
                lastName = "Reid",
                email = "owen.reid@example.com",
                imageUrl = null,
                phone = null,
                department = "Bilgisayar Mühendisliği",
                grade = 3,
                distanceToUniversity = 5f,
                availableTime = 5,
                isSeeker = true,
                matchingStatus = SentRequest(UniqueId("101"))
            ),
        ),
    )

    override suspend fun insertStudent(student: Student) {
        // do nothing
        delay(1000)
        students.add(student)
    }

    override suspend fun getCurrentStudent(): Student {
        delay(1000)
        val studentId = authRepository.getCurrentStudentId()!!
        return students.find { it.uid == studentId }
            ?: error("Kullanıcı bulunamadı")
    }

    override suspend fun getStudent(uid: UniqueId): Student {
        delay(1000)
        return students.find { it.uid == uid }
            ?: error("Kullanıcı bulunamadı")
    }

    override suspend fun updateStudent(student: Student) {
        delay(1000)
        val studentId = authRepository.getCurrentStudentId()!!
        val studentFromDb = students.find { it.uid == studentId }
            ?: error("Kullanıcı bulunamadı")
        students.remove(studentFromDb)
        students.add(student)
    }

    override suspend fun getAllStudents(): List<Student> {
        delay(1000)
        val studentId = authRepository.getCurrentStudentId()!!
        return students.filter { it.uid != studentId }
    }

    override suspend fun sendMatchRequestTo(targetStudentId: UniqueId) {
        delay(500)
        val targetStudent = students.find { it.uid == targetStudentId }
            ?: error("Kullanıcı bulunamadı")
        if (targetStudent.matchingStatus != NoRequest) {
            error("Bu kullanıcıya zaten bir talep gönderilmiş")
        }
        val requestId = UniqueId(Random.nextInt(100..200).toString())
        val targetStudentCopy = targetStudent.clone(matchingStatus = SentRequest(requestId))
        requests.add(
            Sent(
                uid = requestId,
                targetStudent = targetStudentCopy
            )
        )
        students.add(targetStudentCopy)
        students.remove(targetStudent)
        // send notification to targetStudent
        notificationRepository.notifyMatchRequestSent("")
    }

    override suspend fun revokeMatchRequest(requestId: UniqueId) {
        delay(500)
        val targetStudent = requests.find { it.uid == requestId }
            ?.targetStudent
            ?: error("Kullanıcı bulunamadı")
        if (targetStudent.matchingStatus !is SentRequest) {
            error("Bu kullanıcı henüz eşleşme talebi göndermemiş")
        }
        requests.removeIf { it.uid == requestId }
        students.add(targetStudent.clone(matchingStatus = NoRequest))
        students.remove(targetStudent)
    }

    override suspend fun acceptMatchRequest(requestId: UniqueId) {
        delay(500)
        val targetStudent = requests.find { it.uid == requestId }
            ?.targetStudent
            ?: error("Kullanıcı bulunamadı")
        if (targetStudent.matchingStatus !is ReceivedRequest) {
            error("Bu kullanıcıya henüz bir talep gönderilmemiş")
        }
        val targetStudentCopy = targetStudent.clone(matchingStatus = MatchedRequest(requestId))
        requests.removeIf { it.uid == requestId }
        requests.add(Matched(uid = requestId, targetStudent = targetStudentCopy))
        students.add(targetStudentCopy)
        students.remove(targetStudent)
        // send notification to targetStudent
        notificationRepository.notifyMatchRequestAccepted("")
    }

    override suspend fun rejectMatchRequest(requestId: UniqueId) {
        delay(500)
        val targetStudent = requests.find { it.uid == requestId }
            ?.targetStudent
            ?: error("Kullanıcı bulunamadı")
        if (targetStudent.matchingStatus !is ReceivedRequest) {
            error("Bu kullanıcıya henüz bir talep gönderilmemiş")
        }
        requests.removeIf { it.uid == requestId }
        students.add(targetStudent.clone(matchingStatus = NoRequest))
        students.remove(targetStudent)
    }

    override suspend fun agreeMatchRequest(requestId: UniqueId) {
        delay(500)
        val targetStudent = requests.find { it.uid == requestId }
            ?.targetStudent
            ?: error("Kullanıcı bulunamadı")
        if (targetStudent.matchingStatus !is MatchedRequest) {
            error("Bu kullanıcıyla henüz eşleşme sağlanmamış")
        }
        val studentId = authRepository.getCurrentStudentId()!!
        val student = students.find { it.uid == studentId }
            ?: error("Kullanıcı bulunamadı")
        requests.removeIf { it.uid == requestId }
        // change target student type to idle
        students.add(
            Student.Factory.create(
                uid = targetStudent.uid.value,
                firstName = targetStudent.firstName,
                lastName = targetStudent.lastName,
                email = targetStudent.email,
                imageUrl = targetStudent.imageUrl,
                phone = targetStudent.phone,
                department = targetStudent.education?.department,
                grade = targetStudent.education?.grade,
            )
        )
        students.remove(targetStudent)
        // change student type to idle
        students.add(
            Student.Factory.create(
                uid = student.uid.value,
                firstName = student.firstName,
                lastName = student.lastName,
                email = student.email,
                imageUrl = student.imageUrl,
                phone = student.phone,
                department = student.education?.department,
                grade = student.education?.grade,
            )
        )
        students.remove(student)
    }

    override suspend fun disagreeMatchRequest(requestId: UniqueId) {
        delay(500)
        val targetStudent = requests.find { it.uid == requestId }
            ?.targetStudent
            ?: error("Kullanıcı bulunamadı")
        if (targetStudent.matchingStatus !is MatchedRequest) {
            error("Bu kullanıcıyla henüz eşleşme sağlanmamış")
        }
        requests.removeIf { it.uid == requestId }
        students.add(targetStudent.clone(matchingStatus = NoRequest))
        students.remove(targetStudent)
    }

    override suspend fun getReceivedMatchRequests(): List<Received> {
        delay(1000)
        return requests.filterIsInstance<Received>()
    }

    override suspend fun getSentMatchRequests(): List<Sent> {
        delay(1000)
        return requests.filterIsInstance<Sent>()
    }

    override suspend fun getMatchedRequests(): List<Matched> {
        delay(1000)
        return requests.filterIsInstance<Matched>()
    }

    override suspend fun updateFcmToken(token: String) {
        // do nothing
    }

    override suspend fun getFcmToken(targetStudentId: UniqueId): String {
        return "no_token"
    }

    override suspend fun removeFcmToken() {
        // do nothing
    }
}
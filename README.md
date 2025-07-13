# WIDURI E-stock

REST API yang berfungsi untuk manajemen data produk, dan data produk masuk. Dibangun menggunakan Java, Spring Boot, Spring Boot JPA, dan database Mysql.  

## Daftar Isi
* [Tentang Proyek](#tentang-proyek)
* [Fitur Utama](#fitur-utama)
* [Dokumentasi & Live Demo](#dokumentasi--live-demo)
* [Teknologi yang Digunakan](#teknologi-yang-digunakan)
* [Panduan Instalasi & Konfigurasi](#panduan-instalasi--konfigurasi)
* [Contoh Penggunaan API](#contoh-penggunaan-api)
* [Struktur Proyek](#struktur-proyek)
* [Struktur Tabel](#struktur-tabel)
* [Lisensi](#lisensi)
* [Kontak](#kontak)

## Tentang Proyek

Proyek WIDURI E-stock merupakan backend service yang menyediakan serangkaian endpoint API untuk mengelola data pembelian dan inventaris produk pada toko WIDURI Pancing. API ini dirancang sebagai pondasi untuk mengembangkan aplikasi e-commerce pada toko WIDURI Pancing.

Proyek ini dibuat sebagai bagian dari portofolio saya untuk menunjukkan kemampuan dalam merancang dan membangun API yang scalable, aman, dan terdokumentasi dengan baik.

## Fitur Utama
- Authentication & Authorization: Sistem otentikasi menggunakan JWT (Json Web Tokens) dan otorisasi berbasi role (RBAC) dengan tingkatan dari tertinggi ke terendah:
  - Owner
  - Admin Warehouse
  - Admin Seller
  - User
- CRUD Operations: Fungsi penuh untuk membuat, membaca, memperbarui dan menghapus (CRUD) pada resource Products, Incoming Products, Suppliers, dan Users.
- Validasi Request: Menerapkan Fungsi Validasi data pada setiap request untuk memastikan integrasi data yang konsisten dan mudah diakses.
- Pagination: Mendukung paginasi untuk request yang mengembalikan daftar data yang besar.
- Error Handling: Menangani error yang terjadi secara terpusat dan konsisten.
- Upload Gambar: Dapat menangani proses upload gambar Product dan tersimpan di dalam penyimpanan internal aplikasi.
- Dokumentasi: API terdokumentasi dengan lengkap yang menggunakan standar OPEN API 3.0, dan dapat diakses dengan mudah.

## Dokumentasi & Live Demo

Anda dapat melihat dokumentasi lengkap API yang menggunakan Swagger / OpenAPI pada link berikut:

https://api.mfikriproject.my.id/projects/estock/v1/docs

Anda juga dapat mencoba secara langsung endpoint API yang sudah di-deploy di:

Base URL Live: https://api.mfikriproject.my.id/projects/estock/v1

## Teknologi yang digunakan
- Bahasa Pemrograman: Java 21
- Framework: Spring Boot 3.4.5
- Database: Mysql, Spring Data JPA sebagai ORM
- Authentication: Spring Security, JSON Web Token (JWT)
- Validation Data: Jakarta Validation 
- Testing: JUnit5, Spring Boot Test, MockMvc
- Deployment: Docker, Docker Compose, Digital Ocean

## Panduan Instalasi & Konfigurasi

Untuk proses instalasi Anda dapat menggunakan docker atau juga dengan menggunakan Java JDK.

### Instalasi & Konfigurasi menggunakan Docker dan Docker Compose.
1. **Clone repository ini**:

  ```bash
    git clone https://github.com/abdullahfikri/widuri-estock-backend.git && cd widuri-estock-backend
  ```
2. **Setup Environment Variables**
```bash
  export DATABASE_USERNAME=java
  export DATABASE_PASSWORD=java
  export DATABASE_NAME=widuri_estore
  export JWT_SECRET_KEY=702b7afafca85bb203af3eb5d07b0c3a1ad9cee18a4ff72202b227fb989d06aea1ec1beffda5dd20360a05878f926a712a32af61a31559f20ffaf94fc58f77ba7d6d68af7f2fa8dbb0efef750b0b93b0a9e2728f3144fea133c937cb008f1710898dba68ea700e3659f67bc44514ed7f754ac882c20d8dd161de08628032a5a47180914dc9cb72b09f87b1487e415e5a620ac68b3e106befcb6eccf4361e9a29a00e39b536bb92b07f6d27eb6fbf47b5f5d8d3ab3d50d69f80511e958150aada138fa020ebd7df63c7462df31806a471005a715eb12261fbaa4cb02f3ad7d8aab6c59d707f68038cae081db9e15b261e75ca09a94cc7158486b46b06c168cff1
```
*Environment variable ini digunakan untuk mengkonfigurasi Database dan JWT Secret Key.

3. **Jalankan docker compose**
```bash
   docker compose up
   ## atau untuk docker compose versi 1: docker-compose up 
```
*Anda dapat mengakses API dengan Base URL http://localhost:8080/api dan dokumentasi API di http://localhost:8080/api/docs

### Alternative Instalasi dan Konfingurasi menggunakan Java JDK.

*Untuk menjalankan aplikasi ini tanpa docker, anda harus sudah menginstall Java JDK 21 ke atas dan MYSQL Server 8.0 ke atas. 

1. **Clone repository ini**:

  ```bash
    git clone https://github.com/abdullahfikri/widuri-estock-backend.git && cd widuri-estock-backend
  ```

2. **Setup database**

    Anda harus mengkonfigurasi database yang digunakan untuk menyimpan data dari aplikasi.
   * Buat Database Kosong
   
     Buka tool manajemen database Anda (Command Line, phpMyAdmin, MySQL Workbench, dll.) dan buat sebuah database baru dengan nama widuri_estore atau nama lain yang anda pilih sendiri.
     ```mysql
     CREATE DATABASE widuri_estore;
      ```
     * Buat Tabel Database
     
        Selanjutnya anda perlu mengimpor skema tabel yang sudah disediakan. File skema ini berisi semua perintah ``CREATE TABLE`` yang diperlukan.
       <br/>
       <br/> 
        Gunakan file database.sql yang ada pada folder proyek untuk mengisi database yang sudah dibuat tadi. Anda dapat menggunakan command line dengan perintah sebagai berikut:
       
        ```bash
        # Format: mysql -u [username] -p [nama_database] < [file_sql]
        mysql -u root -p widuri_estore < database.sql
       ```
        Setelah menjalankan perintah di atas, anda akan diminta memasukkan password user database Anda. Dan jika berhasil, maka semua tabel yang dibutuhkan akan dibuat di dalam database ``widuri_estore``.
       <br/>
       <br/>
        _Alternatif: Anda juga bisa menggunakan fitur "Import" pada tool GUI seperti phpMyAdmin atau MySQL Workbench. Fitur import tersebut akan meminta anda memilih file ``.sql`` yang akan dijalankan dan Anda dapat memilih file ``database.sql`` yang telah disediakan_


3 **Setup Environment Variables**
```bash
  export DATABASE_USERNAME=java
  export DATABASE_PASSWORD=java
  export DATABASE_NAME=widuri_estore
  export DATABASE_URL=jdbc:mysql://localhost:3306/${DATABASE_NAME}
  export JWT_SECRET_KEY=702b7afafca85bb203af3eb5d07b0c3a1ad9cee18a4ff72202b227fb989d06aea1ec1beffda5dd20360a05878f926a712a32af61a31559f20ffaf94fc58f77ba7d6d68af7f2fa8dbb0efef750b0b93b0a9e2728f3144fea133c937cb008f1710898dba68ea700e3659f67bc44514ed7f754ac882c20d8dd161de08628032a5a47180914dc9cb72b09f87b1487e415e5a620ac68b3e106befcb6eccf4361e9a29a00e39b536bb92b07f6d27eb6fbf47b5f5d8d3ab3d50d69f80511e958150aada138fa020ebd7df63c7462df31806a471005a715eb12261fbaa4cb02f3ad7d8aab6c59d707f68038cae081db9e15b261e75ca09a94cc7158486b46b06c168cff1
```
*Pastikan konfigurasi environment variables sudah sesuai dengan konfigurasi MYSQL Server yang sudah dikonfigurasi sebelumnya. 

4. **Testing & Build**
<br/>
Jalankan perintah berikut untuk memastikan semua fungsi sudah berjalan dan membuat build aplikasi yang akan dijalankan. 
```bash
  ./mvnw clean package
```

5. **Jalankan Aplikasi**

```bash
  java -jar ./target/widuri-estock-0.0.1-SNAPSHOT.jar
```
*Anda dapat mengakses API dengan ``Base URL`` http://localhost:8080/api dan ``Dokumentasi`` API di http://localhost:8080/api/docs

## Contoh Penggunaan API
Saat pertamakali aplikasi dijalankan, Anda sudah disediakan akun `owner` sebagai super user pada aplikasi ini. Akun owner akan digunakan untuk membuat dan mengelola akun lain pada aplikasi.
1. **Login pada akun owner**
<br/>
    * Request: ``POST /auth/login``
    * Body: **JSON**
    ```json
    {
      "username": "owner",
      "password": "owner123",
      "userAgent": "Ubuntu - Mozilla Firefox"
    }
    ```
   Jika berhasil, server akan mengembalikan ``JWT token`` dan waktu expired dalam millisecond yang akan digunakan untuk melakukan request ke semua Endpoint.
   <br/>
    * Success Response (**200 OK**):
    * Body: **JSON**
    ```json
    {
      "data": {
        "token": "randomstring12321312kwjlasdj",
        "expiredAt": 3801299 
      }
    }
    ```
2. **Menggunakan JWT Token**
<br/>
Token yang didapat akan disertakan sebagai ``Authorization Header`` dan menambahkan prefix ``Bearer`` sebelum ``token``  disetiap kali melakukan request ke Endpoint. Berikut adalah contoh yang menyertakan ``JWT Token`` untuk melakukan request ke ``Endpoint`` create category:
    * Request: ``POST /categories``
    * **Header**:
   ```header
    Authorization: Bearer randomstring12321312kwjlasdj
    Content-Type: application/json
    ```
    Body:
    ```json
    {
        "name": "Mata Kail",
        "description": "Hook atau mata kail"
    }
    ```

*Anda dapat melihat Endpoint yang tersedia, struktur request, dan struktur response secara lengkap pada link ``Dokumentasi`` yang telah disediakan di atas.

## Struktur Proyek

```
.
├── src
│   ├── main
│   │   ├── java
│   │   │   └── dev
│   │   │       └── mfikri
│   │   │           └── widuriestock
│   │   │               ├── WiduriEstockApplication.java   
│   │   │               ├── config                  # Berisi Class konfigurasi yang digunakan pada proyek.
│   │   │               ├── constraint              # Kustom Anotasi yang digunakan untuk melakukan validasi khusus (custom validation annotations)  
│   │   │               ├── controller              # Berisi Class yang menerima request HTTP dan memberikan response
│   │   │               ├── converter               # Kustom Class Konverter yang digunakan untuk mengubah data string menjadi data tanggal (DATE)
│   │   │               ├── entity                  # Class-class yang merepresentasikan bentuk data pada Database
│   │   │               ├── entrypoint              # Berisi Class Entry point yang mengecek status authentication request
│   │   │               ├── exception               # Handler untuk custom exception
│   │   │               ├── filter                  # Berisi Class yang menfilter JWT Token yang diterima
│   │   │               ├── listener                # Berisi Class yang akan dieksekusi ketika suatu event berjalan
│   │   │               ├── model                   # Class-class yang merepresentasikan bentuk-bentuk data dari request yang diterima dan response yang akan dikirimkan
│   │   │               ├── repository              # Interface untuk berinteraksi dengan database (JPA)
│   │   │               ├── service                 # Berisi Class yang mengeksekusi logika-logika bisnis utama pada aplikasi
│   │   │               └── util                    # Berisi utilitas yang digunakan untuk membantu class logika bisnis
│   │   └── resources
│   │       ├── static                              # File statis (API Documentation)  
│   │       └── application.yaml                    # Konfigurasi utama aplikasi (port, database, dll.)
│   └── test
│       └── java
│       │   └── dev
│       │       └── mfikri
│       │           └── widuriestock
│       │               └── controller              # Kelas untuk integration test pada class-class controller
│       └── resources
│           └── images                              # File images yang digunakan untuk membantu testing  
├── upload                                          # Folder yang digunakan untuk menyimpan gambar hasil upload
├── .gitignore                                      # File dan folder yang diabaikan oleh Git
├── mvnw                                            # Maven wrapper (untuk build)
├── pom.xml                                         # Konfigurasi proyek dan dependensi Maven
└── README.md                                       # Dokumentasi proyek ini
```

## Struktur Tabel

![ERD](./img/erdx2.png)


## Lisensi

Proyek ini dilisensikan di bawah [Lisensi MIT](LICENSE.md).

## Kontak

Muhammad Fikri - www.linkedin.com/in/mfikridevapp - mfikri.dev@gmail.com

Link Proyek: https://github.com/abdullahfikri/widuri-estock-backend
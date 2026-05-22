# Monitoring BidMart Auth Service

Monitoring di service ini dibuat supaya kondisi aplikasi bisa dipantau saat dijalankan, terutama waktu sedang dites atau diberi beban. Stack yang dipakai adalah:

- Spring Boot Actuator
- Micrometer Prometheus
- Prometheus
- Grafana

## Yang sudah disiapkan

Service ini sudah membuka endpoint berikut:

- `/actuator/health`
- `/actuator/info`
- `/actuator/metrics`
- `/actuator/prometheus`

Selain itu, repo ini juga sudah punya:

- `docker-compose.monitoring.yml` untuk menjalankan Prometheus dan Grafana
- konfigurasi Prometheus untuk scrape metric dari auth-service
- provisioning datasource Grafana ke Prometheus

## Cara menjalankan

1. Jalankan `bidmart-auth-service` di port `8081`.
2. Pastikan endpoint actuator bisa diakses:
   - `http://localhost:8081/actuator/health`
   - `http://localhost:8081/actuator/prometheus`
3. Jalankan stack monitoring:

```bash
docker compose -f docker-compose.monitoring.yml up -d
```

## Akses Grafana

- URL: `http://localhost:3000`
- Username: `admin`
- Password: `admin`

Datasource Prometheus sudah dikonfigurasi otomatis, jadi setelah login tinggal buat dashboard atau panel baru.

## Contoh query yang bisa dipakai

### Request rate seluruh endpoint

```promql
rate(http_server_requests_seconds_count[1m])
```

### Request rate untuk endpoint active sessions

```promql
rate(http_server_requests_seconds_count{uri="/api/profile/sessions"}[1m])
```

### Rata-rata latency endpoint active sessions

```promql
rate(http_server_requests_seconds_sum{uri="/api/profile/sessions"}[1m])
/
rate(http_server_requests_seconds_count{uri="/api/profile/sessions"}[1m])
```

### Penggunaan memori JVM

```promql
jvm_memory_used_bytes
```

### Penggunaan CPU process

```promql
process_cpu_usage
```

### Jumlah thread aktif

```promql
jvm_threads_live_threads
```

## Alasan desain monitoring ini

Monitoring dipasang dengan Spring Boot Actuator dan Micrometer Prometheus karena paling cocok dengan service yang sudah dibangun pakai Spring Boot. Perubahannya kecil, tapi metric yang didapat cukup lengkap untuk kebutuhan pengamatan runtime.

Prometheus dipakai untuk mengambil dan menyimpan metric secara berkala, sedangkan Grafana dipakai untuk menampilkan metric itu dalam bentuk grafik yang lebih mudah dibaca.

Dengan setup ini, kita bisa memantau beberapa hal penting seperti:

- status hidup atau mati service
- throughput request
- latency endpoint tertentu
- penggunaan CPU dan memori
- jumlah thread aktif

## Contoh penggunaan

Monitoring ini bisa dipakai bersamaan dengan load testing. Misalnya saat endpoint `/api/profile/sessions` ditembak menggunakan JMeter, kita bisa melihat perubahan request rate dan latency langsung dari Grafana. Dari situ, dampak sebelum dan sesudah optimasi jadi lebih mudah diamati, bukan hanya dari hasil benchmark, tapi juga dari metric runtime service.

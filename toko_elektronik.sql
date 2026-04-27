-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Waktu pembuatan: 27 Apr 2026 pada 03.15
-- Versi server: 10.4.32-MariaDB
-- Versi PHP: 8.0.30

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `toko_elektronik`
--

-- --------------------------------------------------------

--
-- Struktur dari tabel `barang`
--

CREATE TABLE `barang` (
  `idBarang` varchar(20) NOT NULL,
  `namaBarang` varchar(100) NOT NULL,
  `kategori` varchar(50) DEFAULT NULL,
  `harga` double DEFAULT NULL,
  `stok` int(11) DEFAULT NULL,
  `garansi` varchar(50) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data untuk tabel `barang`
--

INSERT INTO `barang` (`idBarang`, `namaBarang`, `kategori`, `harga`, `stok`, `garansi`) VALUES
('B001', 'Samsung Galaxy A35', 'Handphone', 4500000, 15, '1 Tahun'),
('B0011', 'TE', 'ya', 220000, 10, '11'),
('B002', 'iPhone 15 128GB', 'Handphone', 13500000, 4, '1 Tahun'),
('B003', 'Laptop ASUS VivoBook 15', 'Laptop', 7800000, 12, '2 Tahun'),
('B004', 'Earphone Sony WH-1000XM5', 'Aksesoris', 6500000, 18, '1 Tahun');

-- --------------------------------------------------------

--
-- Struktur dari tabel `customer`
--

CREATE TABLE `customer` (
  `idCustomer` varchar(20) NOT NULL,
  `nama` varchar(100) NOT NULL,
  `telepon` varchar(20) DEFAULT NULL,
  `alamat` varchar(200) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data untuk tabel `customer`
--

INSERT INTO `customer` (`idCustomer`, `nama`, `telepon`, `alamat`) VALUES
('CUST-1776668477926', 'AA', '-', '-'),
('CUST-1776998637395', 'Namaa', '-', '-'),
('CUST-1777209055385', 'NS23421', '-', '-');

-- --------------------------------------------------------

--
-- Struktur dari tabel `detail_penjualan`
--

CREATE TABLE `detail_penjualan` (
  `idDetailPenjualan` varchar(20) NOT NULL,
  `idPenjualan` varchar(20) DEFAULT NULL,
  `idBarang` varchar(20) DEFAULT NULL,
  `jumlah` int(11) DEFAULT NULL,
  `subtotal` double DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data untuk tabel `detail_penjualan`
--

INSERT INTO `detail_penjualan` (`idDetailPenjualan`, `idPenjualan`, `idBarang`, `jumlah`, `subtotal`) VALUES
('DP-0002-01', 'TRX-0002', 'B0011', 1, 220000),
('DP-0003-01', 'TRX-0003', 'B002', 1, 13500000),
('DP-0004-01', 'TRX-0004', 'B002', 1, 13500000),
('DP-0004-02', 'TRX-0004', 'B004', 1, 6500000),
('DP-0005-01', 'TRX-0005', 'B002', 2, 27000000),
('DP-1', 'TRX-0001', 'B004', 1, 6500000);

-- --------------------------------------------------------

--
-- Struktur dari tabel `penjualan`
--

CREATE TABLE `penjualan` (
  `idPenjualan` varchar(20) NOT NULL,
  `tanggal` datetime DEFAULT NULL,
  `idCustomer` varchar(20) DEFAULT NULL,
  `namaCustomer` varchar(100) DEFAULT NULL,
  `subtotal` double DEFAULT NULL,
  `pajak` double DEFAULT NULL,
  `totalAkhir` double DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data untuk tabel `penjualan`
--

INSERT INTO `penjualan` (`idPenjualan`, `tanggal`, `idCustomer`, `namaCustomer`, `subtotal`, `pajak`, `totalAkhir`) VALUES
('TRX-0001', '2026-04-23 15:29:39', 'CUST-1776932979364', 'AA', 6500000, 715000, 7215000),
('TRX-0002', '2026-04-25 03:20:20', 'CUST-1776998637395', 'Namaa', 220000, 22000, 242000),
('TRX-0003', '2026-04-25 03:45:39', 'CUST-1776998637395', 'Namaa', 13500000, 1350000, 14850000),
('TRX-0004', '2026-04-26 13:11:41', 'CUST-1777209055385', 'NS23421', 20000000, 2000000, 22000000),
('TRX-0005', '2026-04-26 13:12:27', 'CUST-1777209055385', 'NS23421', 27000000, 2700000, 29700000);

-- --------------------------------------------------------

--
-- Struktur dari tabel `users`
--

CREATE TABLE `users` (
  `username` varchar(50) NOT NULL,
  `PASSWORD` varchar(50) NOT NULL,
  `role` varchar(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data untuk tabel `users`
--

INSERT INTO `users` (`username`, `PASSWORD`, `role`) VALUES
('admin', '123', 'ADMIN'),
('kasir', '123', 'KASIR');

--
-- Indexes for dumped tables
--

--
-- Indeks untuk tabel `barang`
--
ALTER TABLE `barang`
  ADD PRIMARY KEY (`idBarang`);

--
-- Indeks untuk tabel `customer`
--
ALTER TABLE `customer`
  ADD PRIMARY KEY (`idCustomer`);

--
-- Indeks untuk tabel `detail_penjualan`
--
ALTER TABLE `detail_penjualan`
  ADD PRIMARY KEY (`idDetailPenjualan`),
  ADD KEY `idPenjualan` (`idPenjualan`);

--
-- Indeks untuk tabel `penjualan`
--
ALTER TABLE `penjualan`
  ADD PRIMARY KEY (`idPenjualan`);

--
-- Indeks untuk tabel `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`username`);

--
-- Ketidakleluasaan untuk tabel pelimpahan (Dumped Tables)
--

--
-- Ketidakleluasaan untuk tabel `detail_penjualan`
--
ALTER TABLE `detail_penjualan`
  ADD CONSTRAINT `detail_penjualan_ibfk_1` FOREIGN KEY (`idPenjualan`) REFERENCES `penjualan` (`idPenjualan`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;

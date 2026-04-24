import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DBHelper {

    private static final String DB_URL =
            "jdbc:mysql://localhost:3306/toko_elektronik?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASS = "";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, USER, PASS);
    }

    public static void createTables() {
        String createUsers = """
                CREATE TABLE IF NOT EXISTS users (
                    username VARCHAR(50) PRIMARY KEY,
                    password VARCHAR(50) NOT NULL,
                    role VARCHAR(20) NOT NULL
                ) ENGINE=InnoDB
                """;

        String createBarang = """
                CREATE TABLE IF NOT EXISTS barang (
                    idBarang VARCHAR(20) PRIMARY KEY,
                    namaBarang VARCHAR(100) NOT NULL,
                    kategori VARCHAR(50),
                    harga DOUBLE,
                    stok INT,
                    garansi VARCHAR(50)
                ) ENGINE=InnoDB
                """;

        String createCustomer = """
                CREATE TABLE IF NOT EXISTS customer (
                    idCustomer VARCHAR(20) PRIMARY KEY,
                    nama VARCHAR(100) NOT NULL,
                    telepon VARCHAR(20),
                    alamat VARCHAR(200)
                ) ENGINE=InnoDB
                """;

        String createPenjualan = """
                CREATE TABLE IF NOT EXISTS penjualan (
                    idPenjualan VARCHAR(20) PRIMARY KEY,
                    tanggal DATETIME,
                    idCustomer VARCHAR(20),
                    namaCustomer VARCHAR(100),
                    subtotal DOUBLE,
                    pajak DOUBLE,
                    totalAkhir DOUBLE,
                    CONSTRAINT fk_penjualan_customer
                        FOREIGN KEY (idCustomer) REFERENCES customer(idCustomer)
                        ON UPDATE CASCADE
                        ON DELETE SET NULL
                ) ENGINE=InnoDB
                """;

        String createDetailPenjualan = """
                CREATE TABLE IF NOT EXISTS detail_penjualan (
                    idDetailPenjualan VARCHAR(20) PRIMARY KEY,
                    idPenjualan VARCHAR(20),
                    idBarang VARCHAR(20),
                    jumlah INT,
                    subtotal DOUBLE,
                    CONSTRAINT fk_detail_penjualan_penjualan
                        FOREIGN KEY (idPenjualan) REFERENCES penjualan(idPenjualan)
                        ON UPDATE CASCADE
                        ON DELETE CASCADE,
                    CONSTRAINT fk_detail_penjualan_barang
                        FOREIGN KEY (idBarang) REFERENCES barang(idBarang)
                        ON UPDATE CASCADE
                        ON DELETE RESTRICT
                ) ENGINE=InnoDB
                """;

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(createUsers);
            stmt.execute(createBarang);
            stmt.execute(createCustomer);
            stmt.execute(createPenjualan);
            stmt.execute(createDetailPenjualan);

            System.out.println("✅ Database MySQL & tabel siap.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ================== CUSTOMER ==================

    public static List<String> loadCustomerNames() {
        List<String> list = new ArrayList<>();
        // [FIX] Gunakan kolom spesifik, bukan SELECT *
        String sql = "SELECT nama FROM customer ORDER BY nama";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(rs.getString("nama"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // [FIX] Method addCustomer() lama dihapus karena tidak pernah dipakai
    // (Main.java menggunakan getOrCreateCustomerId() langsung).
    // Diganti dengan insertCustomer() yang menerima objek Customer lengkap
    // agar konsisten jika dibutuhkan di masa depan.
    public static void insertCustomer(Customer c) {
        String sql = "INSERT INTO customer (idCustomer, nama, telepon, alamat) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, c.getIdCustomer());
            pstmt.setString(2, c.getNama());
            pstmt.setString(3, c.getTelepon());
            pstmt.setString(4, c.getAlamat());
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ================== USER ==================

    public static List<User> loadUsers() {
        List<User> list = new ArrayList<>();
        // [FIX] Kolom spesifik menggantikan SELECT *
        String sql = "SELECT username, password, role FROM users";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new User(
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("role")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static void saveUser(User u) {
        String sql = """
                INSERT INTO users (username, password, role)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    password = VALUES(password),
                    role = VALUES(role)
                """;

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, u.getUsername());
            pstmt.setString(2, u.getPassword());
            pstmt.setString(3, u.getRole());
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ================== BARANG ==================

    public static List<Barang> loadInventory() {
        List<Barang> list = new ArrayList<>();
        // [FIX] Kolom spesifik menggantikan SELECT *
        String sql = "SELECT idBarang, namaBarang, kategori, harga, stok, garansi FROM barang";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new Barang(
                        rs.getString("idBarang"),
                        rs.getString("namaBarang"),
                        rs.getString("kategori"),
                        rs.getDouble("harga"),
                        rs.getInt("stok"),
                        rs.getString("garansi")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static void addBarang(Barang b) {
        String sql = "INSERT INTO barang (idBarang, namaBarang, kategori, harga, stok, garansi) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, b.getIdBarang());
            pstmt.setString(2, b.getNamaBarang());
            pstmt.setString(3, b.getKategori());
            pstmt.setDouble(4, b.getHarga());
            pstmt.setInt(5, b.getStok());
            pstmt.setString(6, b.getGaransi());
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateBarang(String idBarang, double harga, int stok) {
        String sql = "UPDATE barang SET harga = ?, stok = ? WHERE idBarang = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, harga);
            pstmt.setInt(2, stok);
            pstmt.setString(3, idBarang);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateStok(String idBarang, int stokBaru) {
        String sql = "UPDATE barang SET stok = ? WHERE idBarang = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, stokBaru);
            pstmt.setString(2, idBarang);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void deleteBarang(String idBarang) {
        String sql = "DELETE FROM barang WHERE idBarang = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, idBarang);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean isBarangUsed(String idBarang) {
        String sql = "SELECT COUNT(*) FROM detail_penjualan WHERE idBarang = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, idBarang);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ================== TRANSAKSI ==================

    public static void savePenjualan(Penjualan p) {
        Connection conn = null;

        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            String insertPenjualan = """
                    INSERT INTO penjualan
                    (idPenjualan, tanggal, idCustomer, namaCustomer, subtotal, pajak, totalAkhir)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """;

            try (PreparedStatement pstmt = conn.prepareStatement(insertPenjualan)) {
                pstmt.setString(1, p.getIdPenjualan());
                pstmt.setTimestamp(2, new Timestamp(p.getTanggal().getTime()));
                pstmt.setString(3, p.getIdCustomer());
                pstmt.setString(4, p.getNamaCustomer());
                pstmt.setDouble(5, p.getSubtotal());
                pstmt.setDouble(6, p.getPajak());
                pstmt.setDouble(7, p.getTotalAkhir());
                pstmt.executeUpdate();
            }

            String insertDetail = """
                    INSERT INTO detail_penjualan
                    (idDetailPenjualan, idPenjualan, idBarang, jumlah, subtotal)
                    VALUES (?, ?, ?, ?, ?)
                    """;

            try (PreparedStatement pstmtD = conn.prepareStatement(insertDetail)) {
                for (PenjualanDetail d : p.getDetailList()) {
                    pstmtD.setString(1, d.getIdDetailPenjualan());
                    pstmtD.setString(2, p.getIdPenjualan());
                    pstmtD.setString(3, d.getIdBarang());
                    pstmtD.setInt(4, d.getJumlah());
                    pstmtD.setDouble(5, d.getSubtotal());
                    pstmtD.executeUpdate();
                }
            }

            conn.commit();
        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * [FIX] N+1 Query Problem diatasi dengan memuat SEMUA detail_penjualan
     * dalam SATU query terpisah, lalu disimpan ke Map<idPenjualan, List<detail>>.
     * Sebelumnya: setiap baris penjualan memicu 1 query tambahan → N+1 total query.
     * Sekarang: hanya 2 query total, tidak peduli berapa banyak transaksi.
     */
    public static List<Penjualan> loadRiwayat() {
        List<Penjualan> list = new ArrayList<>();
        Map<String, List<PenjualanDetail>> detailMap = new HashMap<>();

        try (Connection conn = getConnection()) {

            // Query 1: Muat semua detail sekaligus
            String sqlDetail = "SELECT idDetailPenjualan, idPenjualan, idBarang, jumlah, subtotal FROM detail_penjualan";
            try (Statement stmtD = conn.createStatement();
                 ResultSet rsD = stmtD.executeQuery(sqlDetail)) {

                while (rsD.next()) {
                    String idPenj = rsD.getString("idPenjualan");
                    PenjualanDetail detail = new PenjualanDetail(
                            rsD.getString("idDetailPenjualan"),
                            rsD.getString("idBarang"),
                            rsD.getInt("jumlah"),
                            rsD.getDouble("subtotal")
                    );
                    // computeIfAbsent: buat list baru jika key belum ada, lalu tambahkan detail
                    detailMap.computeIfAbsent(idPenj, k -> new ArrayList<>()).add(detail);
                }
            }

            // Query 2: Muat semua penjualan, pasangkan dengan detail dari Map (tanpa query tambahan)
            String sqlPenjualan = "SELECT idPenjualan, tanggal, idCustomer, namaCustomer FROM penjualan ORDER BY tanggal DESC";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sqlPenjualan)) {

                while (rs.next()) {
                    String idPenj = rs.getString("idPenjualan");
                    Timestamp ts = rs.getTimestamp("tanggal");
                    java.util.Date tanggal = (ts != null) ? new java.util.Date(ts.getTime()) : new java.util.Date();
                    String idCust = rs.getString("idCustomer");
                    String namaCust = rs.getString("namaCustomer");

                    // Ambil detail dari map, default empty list jika tidak ada
                    List<PenjualanDetail> details = detailMap.getOrDefault(idPenj, new ArrayList<>());

                    Penjualan p = new Penjualan(idPenj, tanggal, idCust, namaCust, details);
                    p.hitungTotal();
                    list.add(p);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public static void deletePenjualan(String idPenjualan) {
        Connection conn = null;

        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(
                    "DELETE FROM detail_penjualan WHERE idPenjualan = ?")) {
                pstmt.setString(1, idPenjualan);
                pstmt.executeUpdate();
            }

            try (PreparedStatement pstmt = conn.prepareStatement(
                    "DELETE FROM penjualan WHERE idPenjualan = ?")) {
                pstmt.setString(1, idPenjualan);
                pstmt.executeUpdate();
            }

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void updateNamaCustomer(String idPenjualan, String newNama) {
        String sql = "UPDATE penjualan SET namaCustomer = ? WHERE idPenjualan = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newNama);
            pstmt.setString(2, idPenjualan);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
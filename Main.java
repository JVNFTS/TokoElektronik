import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    private static List<Barang> inventory = new ArrayList<>();
    private static List<Penjualan> riwayatTransaksi = new ArrayList<>();
    private static List<User> users = new ArrayList<>();
    private static int transaksiCounter = 1;

    public static void main(String[] args) {
        inisialisasiData();

        // Semua komponen Swing harus dibuat di Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            User userLogin = loginGUI();
            if (userLogin == null) {
                JOptionPane.showMessageDialog(null, "Program ditutup.");
                return;
            }
            createMainUI(userLogin);
        });
    }

    private static void inisialisasiData() {
        DBHelper.createTables();

        users = DBHelper.loadUsers();
        if (users.isEmpty()) {
            users.add(new User("admin", "123", "ADMIN"));
            users.add(new User("kasir", "123", "KASIR"));
            for (User u : users) DBHelper.saveUser(u);
        }

        inventory = DBHelper.loadInventory();
        if (inventory.isEmpty()) {
            Barang[] initial = {
                    new Barang("B001", "Samsung Galaxy A35", "Handphone", 4500000, 15, "1 Tahun"),
                    new Barang("B002", "iPhone 15 128GB", "Handphone", 13500000, 8, "1 Tahun"),
                    new Barang("B003", "Laptop ASUS VivoBook 15", "Laptop", 7800000, 12, "2 Tahun"),
                    new Barang("B004", "Earphone Sony WH-1000XM5", "Aksesoris", 6500000, 20, "1 Tahun")
            };
            for (Barang b : initial) {
                inventory.add(b);
                DBHelper.addBarang(b);
            }
        }

        riwayatTransaksi = DBHelper.loadRiwayat();
        transaksiCounter = getNextTransaksiCounter();
    }

    private static int getNextTransaksiCounter() {
        String sql = "SELECT MAX(CAST(SUBSTRING(idPenjualan, 5) AS UNSIGNED)) FROM penjualan";
        try (Connection conn = DBHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                int max = rs.getInt(1);
                return max + 1;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 1;
    }

    private static User loginGUI() {
        while (true) {
            JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
            JTextField userField = new JTextField(15);
            JPasswordField passField = new JPasswordField(15);

            panel.add(new JLabel("Username :"));
            panel.add(userField);
            panel.add(new JLabel("Password :"));
            panel.add(passField);

            int option = JOptionPane.showConfirmDialog(null, panel,
                    "Login Toko Elektronik", JOptionPane.OK_CANCEL_OPTION);
            if (option != JOptionPane.OK_OPTION) return null;

            String username = userField.getText().trim();
            String password = new String(passField.getPassword()).trim();

            for (User u : users) {
                if (u.login(username, password)) {
                    JOptionPane.showMessageDialog(null,
                            "Login BERHASIL!\nSelamat datang, " + u.getRole());
                    return u;
                }
            }
            JOptionPane.showMessageDialog(null,
                    "Username atau password SALAH!", "Gagal", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static JMenuItem createStyledMenuItem(String text) {
        JMenuItem item = new JMenuItem(text);
        item.setFont(new Font("Arial", Font.BOLD, 14));
        item.setBackground(new Color(0, 102, 204));
        item.setForeground(Color.WHITE);
        return item;
    }

    private static void createMainUI(User userLogin) {
        JFrame mainFrame = new JFrame("Toko Elektronik - " + userLogin.getRole());
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(950, 620);
        mainFrame.setLocationRelativeTo(null);

        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Fitur Program");

        JMenuItem itemCekStok = createStyledMenuItem("1. Cek Stok & Harga");
        itemCekStok.addActionListener(e -> cekStokDanHargaGUI());
        menu.add(itemCekStok);

        if (userLogin.getRole().equals("ADMIN")) {
            JMenuItem itemManajemen = createStyledMenuItem("2. Manajemen Barang");
            itemManajemen.addActionListener(e -> manajemenBarangGUI());
            menu.add(itemManajemen);
        } else {
            JMenuItem itemTransaksi = createStyledMenuItem("2. Transaksi Penjualan (Kasir)");
            itemTransaksi.addActionListener(e -> transaksiPenjualanGUI(userLogin));
            menu.add(itemTransaksi);
        }

        // Teruskan userLogin agar riwayat tahu apakah perlu tampilkan tombol edit/hapus
        JMenuItem itemRiwayat = createStyledMenuItem("3. Riwayat Transaksi");
        itemRiwayat.addActionListener(e -> riwayatTransaksiGUI(userLogin));
        menu.add(itemRiwayat);

        JMenuItem itemLaporan = createStyledMenuItem("4. Laporan Stok Rendah");
        itemLaporan.addActionListener(e -> laporanStokRendahGUI());
        menu.add(itemLaporan);

        JMenuItem itemKeluar = createStyledMenuItem("5. Keluar");
        itemKeluar.addActionListener(e -> logout(mainFrame));
        menu.add(itemKeluar);

        menuBar.add(menu);
        mainFrame.setJMenuBar(menuBar);

        Image bgImage = Toolkit.getDefaultToolkit().getImage("img1.png");
        BackgroundPanel bgPanel = new BackgroundPanel(bgImage);
        bgPanel.setOpacity(0.30f);
        bgPanel.setScaleMode(BackgroundPanel.ScaleMode.COVER);

        mainFrame.setContentPane(bgPanel);
        bgPanel.setLayout(new BorderLayout());

        JLabel welcome = new JLabel(
                "<html><h2>Selamat datang, " + userLogin.getRole()
                + "!<br>Silakan pilih fitur dari menu di atas.</h2></html>",
                SwingConstants.CENTER);
        bgPanel.add(welcome, BorderLayout.CENTER);

        mainFrame.setVisible(true);
    }

    private static void logout(JFrame frame) {
        frame.dispose();
        inisialisasiData();
        SwingUtilities.invokeLater(() -> {
            User userLogin = loginGUI();
            if (userLogin != null) createMainUI(userLogin);
        });
    }

    // ============================================================
    //  TRANSAKSI PENJUALAN — Barang dipilih lewat JComboBox
    // ============================================================

    private static void transaksiPenjualanGUI(User kasir) {
        List<PenjualanDetail> cart = new ArrayList<>();

        // ---- Pilih customer ----
        List<String> customerNames = DBHelper.loadCustomerNames();
        JComboBox<String> comboCustomer = new JComboBox<>();
        comboCustomer.addItem("--- Pilih Customer ---");
        for (String nama : customerNames) comboCustomer.addItem(nama);
        comboCustomer.addItem("Tambah Customer Baru");

        int result = JOptionPane.showConfirmDialog(null, comboCustomer,
                "Pilih atau Tambah Customer", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) {
            JOptionPane.showMessageDialog(null, "Transaksi dibatalkan.");
            return;
        }

        String namaCust = (String) comboCustomer.getSelectedItem();
        if (namaCust == null || namaCust.equals("--- Pilih Customer ---")) {
            JOptionPane.showMessageDialog(null, "Transaksi dibatalkan.");
            return;
        }

        String customerId;
        if (namaCust.equals("Tambah Customer Baru")) {
            namaCust = JOptionPane.showInputDialog(null,
                    "Masukkan Nama Customer Baru:", "Customer Baru", JOptionPane.QUESTION_MESSAGE);
            if (namaCust == null || namaCust.trim().isEmpty()) return;
            namaCust = namaCust.trim();
            customerId = getOrCreateCustomerId(namaCust);
            if (customerId == null) {
                JOptionPane.showMessageDialog(null, "Gagal menambahkan customer.");
                return;
            }
        } else {
            customerId = getCustomerIdByName(namaCust);
            if (customerId == null) {
                JOptionPane.showMessageDialog(null, "ID customer tidak ditemukan di database.");
                return;
            }
        }

        // ---- Loop keranjang ----
        final String namaCustomerFinal = namaCust;
        final String customerIdFinal   = customerId;

        boolean selesai = false;
        while (!selesai) {
            String[] options = {"Tambah Barang ke Keranjang", "Lihat Keranjang", "Selesai & Bayar", "Batal"};
            int pilihan = JOptionPane.showOptionDialog(
                    null,
                    "Keranjang saat ini: " + cart.size() + " item\nNama Customer: " + namaCustomerFinal,
                    "Transaksi Penjualan",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                    null, options, options[0]
            );

            if (pilihan == 0) {
                // Hanya tampilkan barang yang masih ada stoknya
                List<Barang> tersedia = new ArrayList<>();
                for (Barang b : inventory) {
                    if ((b.getStok() - getQtyInCart(cart, b.getIdBarang())) > 0) tersedia.add(b);
                }

                if (tersedia.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Tidak ada barang yang tersedia!");
                    continue;
                }

                // Label: "B001  |  Samsung Galaxy A35  |  Rp 4.500.000  |  Stok tersedia: 15"
                String[] labelBarang = new String[tersedia.size()];
                for (int i = 0; i < tersedia.size(); i++) {
                    Barang b = tersedia.get(i);
                    int sisa = b.getStok() - getQtyInCart(cart, b.getIdBarang());
                    labelBarang[i] = String.format("%s  |  %s  |  Rp %,.0f  |  Stok tersedia: %d",
                            b.getIdBarang(), b.getNamaBarang(), b.getHarga(), sisa);
                }

                JComboBox<String> comboBarang = new JComboBox<>(labelBarang);
                comboBarang.setPreferredSize(new Dimension(480, 28));

                int res = JOptionPane.showConfirmDialog(null, comboBarang,
                        "Pilih Barang", JOptionPane.OK_CANCEL_OPTION);
                if (res != JOptionPane.OK_OPTION) continue;

                int idx = comboBarang.getSelectedIndex();
                if (idx < 0) continue;

                Barang barangTerpilih = tersedia.get(idx);
                int stokSisa = barangTerpilih.getStok() - getQtyInCart(cart, barangTerpilih.getIdBarang());

                // Spinner jumlah (1 s.d. stok sisa)
                SpinnerNumberModel spinnerModel = new SpinnerNumberModel(1, 1, stokSisa, 1);
                JSpinner spinnerJumlah = new JSpinner(spinnerModel);

                JPanel panelQty = new JPanel(new GridLayout(2, 1, 5, 5));
                panelQty.add(new JLabel("Jumlah beli: " + barangTerpilih.getNamaBarang()
                        + "  (maks. " + stokSisa + ")"));
                panelQty.add(spinnerJumlah);

                int resQty = JOptionPane.showConfirmDialog(null, panelQty,
                        "Jumlah Pembelian", JOptionPane.OK_CANCEL_OPTION);
                if (resQty != JOptionPane.OK_OPTION) continue;

                int qty = (int) spinnerJumlah.getValue();
                String idDetail = "DP-" + String.format("%04d", transaksiCounter)
                        + "-" + String.format("%02d", cart.size() + 1);
                PenjualanDetail detail = new PenjualanDetail(
                        idDetail, barangTerpilih.getIdBarang(), qty);
                detail.hitungSubtotal(barangTerpilih.getHarga());
                cart.add(detail);

                JOptionPane.showMessageDialog(null,
                        barangTerpilih.getNamaBarang() + " x " + qty + " berhasil ditambahkan!");

            } else if (pilihan == 1) {
                if (cart.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Keranjang masih kosong.");
                    continue;
                }
                StringBuilder sb = new StringBuilder("=== KERANJANG BELANJA ===\n\n");
                double total = 0;
                for (PenjualanDetail d : cart) {
                    Barang b = findBarangById(d.getIdBarang());
                    if (b != null) {
                        sb.append(b.getNamaBarang()).append(" x ").append(d.getJumlah())
                          .append(" = Rp ").append(String.format("%,.0f", d.getSubtotal())).append("\n");
                        total += d.getSubtotal();
                    }
                }
                sb.append("\nTotal sementara : Rp ").append(String.format("%,.0f", total));
                JOptionPane.showMessageDialog(null, sb.toString(), "Keranjang", JOptionPane.PLAIN_MESSAGE);

            } else if (pilihan == 2) {
                if (cart.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Keranjang kosong! Tambah barang dulu.");
                    continue;
                }
                int confirm = JOptionPane.showConfirmDialog(null,
                        "Simpan transaksi dengan " + cart.size() + " item?\nTotal akan dihitung otomatis.",
                        "Konfirmasi Pembelian", JOptionPane.YES_NO_OPTION);
                if (confirm != JOptionPane.YES_OPTION) continue;

                String idTrans = "TRX-" + String.format("%04d", transaksiCounter);
                Penjualan transaksi = new Penjualan(idTrans, customerIdFinal, namaCustomerFinal, cart);
                transaksi.hitungTotal();

                boolean sukses = simpanTransaksiKeDatabase(transaksi);
                if (sukses) {
                    transaksiCounter++;
                    String struk = transaksi.getStrukText(inventory.toArray(new Barang[0]));
                    JTextArea ta = new JTextArea(struk);
                    ta.setEditable(false);
                    JOptionPane.showMessageDialog(null, new JScrollPane(ta),
                            "STRUK PENJUALAN", JOptionPane.INFORMATION_MESSAGE);
                    riwayatTransaksi.add(transaksi);
                    JOptionPane.showMessageDialog(null, "✅ Transaksi berhasil disimpan!");
                    selesai = true;
                } else {
                    JOptionPane.showMessageDialog(null, "❌ Transaksi gagal disimpan.");
                }
            } else {
                selesai = true;
            }
        }
    }

    private static boolean simpanTransaksiKeDatabase(Penjualan transaksi) {
        Connection conn = null;
        try {
            conn = DBHelper.getConnection();
            conn.setAutoCommit(false);

            Map<String, Integer> totalQtyPerBarang = new HashMap<>();
            for (PenjualanDetail d : transaksi.getDetailList()) {
                totalQtyPerBarang.put(d.getIdBarang(),
                        totalQtyPerBarang.getOrDefault(d.getIdBarang(), 0) + d.getJumlah());
            }

            for (Map.Entry<String, Integer> entry : totalQtyPerBarang.entrySet()) {
                Barang b = findBarangById(entry.getKey());
                if (b == null) throw new SQLException("Barang tidak ditemukan: " + entry.getKey());
                if (b.getStok() < entry.getValue())
                    throw new SQLException("Stok tidak cukup: " + b.getNamaBarang());
            }

            try (PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO penjualan (idPenjualan,tanggal,idCustomer,namaCustomer,subtotal,pajak,totalAkhir) VALUES (?,?,?,?,?,?,?)")) {
                pstmt.setString(1, transaksi.getIdPenjualan());
                pstmt.setTimestamp(2, new Timestamp(transaksi.getTanggal().getTime()));
                pstmt.setString(3, transaksi.getIdCustomer());
                pstmt.setString(4, transaksi.getNamaCustomer());
                pstmt.setDouble(5, transaksi.getSubtotal());
                pstmt.setDouble(6, transaksi.getPajak());
                pstmt.setDouble(7, transaksi.getTotalAkhir());
                pstmt.executeUpdate();
            }

            try (PreparedStatement pd = conn.prepareStatement(
                    "INSERT INTO detail_penjualan (idDetailPenjualan,idPenjualan,idBarang,jumlah,subtotal) VALUES (?,?,?,?,?)");
                 PreparedStatement ps = conn.prepareStatement(
                    "UPDATE barang SET stok = stok - ? WHERE idBarang = ?")) {

                for (PenjualanDetail d : transaksi.getDetailList()) {
                    pd.setString(1, d.getIdDetailPenjualan());
                    pd.setString(2, transaksi.getIdPenjualan());
                    pd.setString(3, d.getIdBarang());
                    pd.setInt(4, d.getJumlah());
                    pd.setDouble(5, d.getSubtotal());
                    pd.executeUpdate();
                }
                for (Map.Entry<String, Integer> entry : totalQtyPerBarang.entrySet()) {
                    ps.setInt(1, entry.getValue());
                    ps.setString(2, entry.getKey());
                    ps.executeUpdate();
                }
            }

            conn.commit();

            for (Map.Entry<String, Integer> entry : totalQtyPerBarang.entrySet()) {
                Barang b = findBarangById(entry.getKey());
                if (b != null) b.setStok(b.getStok() - entry.getValue());
            }
            return true;

        } catch (Exception e) {
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
            return false;
        } finally {
            try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    // ============================================================
    //  RIWAYAT TRANSAKSI — Edit & Hapus HANYA untuk ADMIN
    // ============================================================

    private static void riwayatTransaksiGUI(User user) {
        if (riwayatTransaksi.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Belum ada transaksi.");
            return;
        }

        boolean isAdmin = user.getRole().equals("ADMIN");

        String[] col = {"ID Transaksi", "Tanggal", "Customer", "Total"};
        DefaultTableModel model = new DefaultTableModel(col, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (Penjualan p : riwayatTransaksi) {
            model.addRow(new Object[]{
                    p.getIdPenjualan(), p.getTanggal(),
                    p.getNamaCustomer(), String.format("Rp %,.0f", p.getTotalAkhir())
            });
        }

        JTable table = new JTable(model);
        JScrollPane scroll = new JScrollPane(table);

        JDialog dialog = new JDialog((Frame) null, "Riwayat Transaksi", true);
        dialog.setLayout(new BorderLayout());
        dialog.add(scroll, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));

        if (isAdmin) {
            // ADMIN: tampilkan tombol Edit dan Hapus
            JButton btnEdit  = new JButton("✏️  Edit Transaksi");
            JButton btnHapus = new JButton("🗑️  Hapus Transaksi");

            btnEdit.addActionListener(e -> {
                int row = table.getSelectedRow();
                if (row == -1) { JOptionPane.showMessageDialog(dialog, "Pilih transaksi dulu!"); return; }
                String id = (String) model.getValueAt(row, 0);
                if (editTransaksiById(id, dialog)) {
                    for (Penjualan p : riwayatTransaksi) {
                        if (p.getIdPenjualan().equals(id)) {
                            model.setValueAt(p.getNamaCustomer(), row, 2); break;
                        }
                    }
                }
            });

            btnHapus.addActionListener(e -> {
                int row = table.getSelectedRow();
                if (row == -1) { JOptionPane.showMessageDialog(dialog, "Pilih transaksi dulu!"); return; }
                String id = (String) model.getValueAt(row, 0);
                if (hapusTransaksiById(id, dialog)) model.removeRow(row);
            });

            buttonPanel.add(btnEdit);
            buttonPanel.add(btnHapus);
        } else {
            // KASIR: hanya tampilkan label informasi, tanpa tombol edit/hapus
            JLabel info = new JLabel("ℹ️  Mode Kasir: riwayat hanya dapat dilihat.");
            info.setForeground(new Color(180, 60, 0));
            info.setFont(new Font("Arial", Font.ITALIC, 12));
            buttonPanel.add(info);
        }

        JButton btnTutup = new JButton("Tutup");
        btnTutup.addActionListener(e -> dialog.dispose());
        buttonPanel.add(btnTutup);

        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setSize(850, 550);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    // ============================================================
    //  CEK STOK
    // ============================================================

    private static void cekStokDanHargaGUI() {
        String[] col = {"ID", "Nama Barang", "Kategori", "Harga", "Stok"};
        Object[][] data = new Object[inventory.size()][5];
        for (int i = 0; i < inventory.size(); i++) {
            Barang b = inventory.get(i);
            data[i] = new Object[]{ b.getIdBarang(), b.getNamaBarang(),
                    b.getKategori(), String.format("Rp %,.0f", b.getHarga()), b.getStok() };
        }
        JTable table = new JTable(data, col);
        JOptionPane.showMessageDialog(null, new JScrollPane(table),
                "DAFTAR BARANG & STOK", JOptionPane.PLAIN_MESSAGE);
    }

    // ============================================================
    //  MANAJEMEN BARANG (ADMIN) — Barang dipilih lewat JComboBox
    // ============================================================

    private static void manajemenBarangGUI() {
        String[] pilihan = {"Tambah Barang Baru", "Edit Harga / Stok", "Hapus Barang", "Kembali"};
        int p = JOptionPane.showOptionDialog(null, "Pilih aksi:", "Manajemen Barang (ADMIN)",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, pilihan, pilihan[0]);

        if (p == 0) tambahBarangBaruGUI();
        else if (p == 1) editBarangGUI();
        else if (p == 2) hapusBarangGUI();
    }

    private static void tambahBarangBaruGUI() {
        JTextField idF      = new JTextField();
        JTextField namaF    = new JTextField();
        JTextField katF     = new JTextField();
        JTextField hargaF   = new JTextField();
        JTextField stokF    = new JTextField();
        JTextField garansiF = new JTextField();

        Object[] msg = {
                "ID Barang:", idF, "Nama Barang:", namaF, "Kategori:", katF,
                "Harga Jual (Rp):", hargaF, "Stok Awal:", stokF, "Garansi (opsional):", garansiF
        };

        if (JOptionPane.showConfirmDialog(null, msg, "Tambah Barang Baru",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

        String id       = idF.getText().trim();
        String nama     = namaF.getText().trim();
        String kategori = katF.getText().trim();
        String garansi  = garansiF.getText().trim();

        if (id.isEmpty() || nama.isEmpty() || kategori.isEmpty()) {
            JOptionPane.showMessageDialog(null, "ID Barang, Nama, dan Kategori harus diisi!");
            return;
        }
        for (Barang b : inventory) {
            if (b.getIdBarang().equalsIgnoreCase(id)) {
                JOptionPane.showMessageDialog(null, "❌ ID Barang sudah ada!");
                return;
            }
        }

        try {
            double harga = Double.parseDouble(hargaF.getText().trim());
            int stok     = Integer.parseInt(stokF.getText().trim());
            if (harga < 0 || stok < 0) {
                JOptionPane.showMessageDialog(null, "Harga dan stok tidak boleh negatif!");
                return;
            }
            Barang baru = new Barang(id, nama, kategori, harga, stok, garansi);
            inventory.add(baru);
            DBHelper.addBarang(baru);
            JOptionPane.showMessageDialog(null, "✅ Barang berhasil ditambahkan!");
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Input harga/stok tidak valid!");
        }
    }

    /**
     * Edit barang: pilih lewat JComboBox, lalu ubah harga/stok via form.
     */
    private static void editBarangGUI() {
        if (inventory.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Belum ada barang di inventory.");
            return;
        }

        // Label: "B001  |  Samsung Galaxy A35  |  Rp 4.500.000  |  Stok: 15"
        String[] labels = new String[inventory.size()];
        for (int i = 0; i < inventory.size(); i++) {
            Barang b = inventory.get(i);
            labels[i] = String.format("%s  |  %s  |  Rp %,.0f  |  Stok: %d",
                    b.getIdBarang(), b.getNamaBarang(), b.getHarga(), b.getStok());
        }

        JComboBox<String> combo = new JComboBox<>(labels);
        combo.setPreferredSize(new Dimension(480, 28));

        int res = JOptionPane.showConfirmDialog(null, combo,
                "Pilih Barang yang akan Diedit", JOptionPane.OK_CANCEL_OPTION);
        if (res != JOptionPane.OK_OPTION) return;

        int idx = combo.getSelectedIndex();
        if (idx < 0) return;

        Barang target = inventory.get(idx);

        JTextField hargaF = new JTextField(String.valueOf(target.getHarga()));
        JTextField stokF  = new JTextField(String.valueOf(target.getStok()));
        Object[] msg = {
                "Barang  : " + target.getNamaBarang(),
                "Harga baru (Rp):", hargaF,
                "Stok baru:", stokF
        };

        if (JOptionPane.showConfirmDialog(null, msg, "Edit Barang",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

        try {
            double harga = Double.parseDouble(hargaF.getText().trim());
            int stok     = Integer.parseInt(stokF.getText().trim());
            if (harga < 0 || stok < 0) {
                JOptionPane.showMessageDialog(null, "Harga dan stok tidak boleh negatif!");
                return;
            }
            target.setHarga(harga);
            target.setStok(stok);
            DBHelper.updateBarang(target.getIdBarang(), harga, stok);
            JOptionPane.showMessageDialog(null, "✅ Barang berhasil diupdate!");
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Input tidak valid!");
        }
    }

    /**
     * Hapus barang: pilih lewat JComboBox.
     * Hanya barang yang BELUM pernah dijual yang ditampilkan.
     */
    private static void hapusBarangGUI() {
        if (inventory.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Belum ada barang di inventory.");
            return;
        }

        List<Barang> bisaDihapus = new ArrayList<>();
        for (Barang b : inventory) {
            if (!DBHelper.isBarangUsed(b.getIdBarang())) bisaDihapus.add(b);
        }

        if (bisaDihapus.isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "Semua barang sudah pernah dijual.\nTidak ada barang yang bisa dihapus.",
                    "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String[] labels = new String[bisaDihapus.size()];
        for (int i = 0; i < bisaDihapus.size(); i++) {
            Barang b = bisaDihapus.get(i);
            labels[i] = String.format("%s  |  %s  |  Stok: %d",
                    b.getIdBarang(), b.getNamaBarang(), b.getStok());
        }

        JComboBox<String> combo = new JComboBox<>(labels);
        combo.setPreferredSize(new Dimension(420, 28));

        int res = JOptionPane.showConfirmDialog(null, combo,
                "Pilih Barang yang akan Dihapus", JOptionPane.OK_CANCEL_OPTION);
        if (res != JOptionPane.OK_OPTION) return;

        int idx = combo.getSelectedIndex();
        if (idx < 0) return;

        Barang target = bisaDihapus.get(idx);

        int confirm = JOptionPane.showConfirmDialog(null,
                "Yakin ingin menghapus:\n" + target.getNamaBarang()
                + " (" + target.getIdBarang() + ")?",
                "Konfirmasi Hapus", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        inventory.remove(target);
        DBHelper.deleteBarang(target.getIdBarang());
        JOptionPane.showMessageDialog(null, "✅ Barang berhasil dihapus!");
    }

    private static void laporanStokRendahGUI() {
        StringBuilder sb = new StringBuilder("=== LAPORAN STOK RENDAH (< 5) ===\n\n");
        boolean ada = false;
        for (Barang b : inventory) {
            if (b.getStok() < 5) {
                sb.append(String.format("%s - %s | Stok: %d\n",
                        b.getIdBarang(), b.getNamaBarang(), b.getStok()));
                ada = true;
            }
        }
        if (!ada) sb.append("Semua stok masih aman.");
        JTextArea ta = new JTextArea(sb.toString());
        ta.setEditable(false);
        JOptionPane.showMessageDialog(null, new JScrollPane(ta),
                "Laporan Stok Rendah", JOptionPane.PLAIN_MESSAGE);
    }

    // ============================================================
    //  EDIT & HAPUS TRANSAKSI (hanya bisa dipanggil oleh ADMIN)
    // ============================================================

    private static boolean editTransaksiById(String id, Component parent) {
        for (Penjualan p : riwayatTransaksi) {
            if (p.getIdPenjualan().equals(id)) {
                String namaBaru = JOptionPane.showInputDialog(parent,
                        "Nama Customer baru:", p.getNamaCustomer());
                if (namaBaru != null && !namaBaru.trim().isEmpty()) {
                    namaBaru = namaBaru.trim();
                    p.setNamaCustomer(namaBaru);
                    DBHelper.updateNamaCustomer(id, namaBaru);
                    JOptionPane.showMessageDialog(parent, "✅ Nama customer berhasil diupdate!");
                    return true;
                }
                return false;
            }
        }
        JOptionPane.showMessageDialog(parent, "Transaksi tidak ditemukan!",
                "Error", JOptionPane.ERROR_MESSAGE);
        return false;
    }

    private static boolean hapusTransaksiById(String id, Component parent) {
        Penjualan target = null;
        for (Penjualan p : riwayatTransaksi) {
            if (p.getIdPenjualan().equals(id)) { target = p; break; }
        }
        if (target == null) {
            JOptionPane.showMessageDialog(parent, "Transaksi tidak ditemukan!",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        int confirm = JOptionPane.showConfirmDialog(parent,
                "Hapus transaksi " + id + "?\nStok barang akan dikembalikan.\n\nApakah Anda yakin?",
                "Konfirmasi Hapus", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return false;

        Connection conn = null;
        try {
            Map<String, Integer> qtyMap = new HashMap<>();
            for (PenjualanDetail d : target.getDetailList())
                qtyMap.put(d.getIdBarang(), qtyMap.getOrDefault(d.getIdBarang(), 0) + d.getJumlah());

            conn = DBHelper.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement p1 = conn.prepareStatement(
                    "DELETE FROM detail_penjualan WHERE idPenjualan = ?")) {
                p1.setString(1, id); p1.executeUpdate();
            }
            try (PreparedStatement p2 = conn.prepareStatement(
                    "DELETE FROM penjualan WHERE idPenjualan = ?")) {
                p2.setString(1, id); p2.executeUpdate();
            }
            try (PreparedStatement p3 = conn.prepareStatement(
                    "UPDATE barang SET stok = stok + ? WHERE idBarang = ?")) {
                for (Map.Entry<String, Integer> e : qtyMap.entrySet()) {
                    p3.setInt(1, e.getValue()); p3.setString(2, e.getKey()); p3.executeUpdate();
                }
            }

            conn.commit();

            for (Map.Entry<String, Integer> e : qtyMap.entrySet()) {
                Barang b = findBarangById(e.getKey());
                if (b != null) b.setStok(b.getStok() + e.getValue());
            }
            riwayatTransaksi.remove(target);
            JOptionPane.showMessageDialog(parent, "✅ Transaksi berhasil dihapus dan stok dikembalikan!");
            return true;

        } catch (Exception e) {
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
            JOptionPane.showMessageDialog(parent, "❌ Gagal menghapus transaksi.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        } finally {
            try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    // ============================================================
    //  HELPER
    // ============================================================

    private static Barang findBarangById(String idBarang) {
        for (Barang b : inventory)
            if (b.getIdBarang().equalsIgnoreCase(idBarang)) return b;
        return null;
    }

    private static int getQtyInCart(List<PenjualanDetail> cart, String idBarang) {
        int total = 0;
        for (PenjualanDetail d : cart)
            if (d.getIdBarang().equalsIgnoreCase(idBarang)) total += d.getJumlah();
        return total;
    }

    private static String getCustomerIdByName(String nama) {
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                "SELECT idCustomer FROM customer WHERE nama = ? ORDER BY idCustomer DESC LIMIT 1")) {
            pstmt.setString(1, nama);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getString("idCustomer");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    private static String getOrCreateCustomerId(String nama) {
        String id = getCustomerIdByName(nama);
        if (id != null) return id;

        id = "CUST-" + System.currentTimeMillis();
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO customer (idCustomer, nama, telepon, alamat) VALUES (?, ?, ?, ?)")) {
            pstmt.setString(1, id);
            pstmt.setString(2, nama);
            pstmt.setString(3, "-");
            pstmt.setString(4, "-");
            pstmt.executeUpdate();
            return id;
        } catch (SQLException e) { e.printStackTrace(); return null; }
    }
}
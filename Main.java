import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
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
                    new Barang("B001", "Samsung Galaxy A35",      "Handphone", 4500000,  15, "1 Tahun"),
                    new Barang("B002", "iPhone 15 128GB",          "Handphone", 13500000,  8, "1 Tahun"),
                    new Barang("B003", "Laptop ASUS VivoBook 15",  "Laptop",     7800000, 12, "2 Tahun"),
                    new Barang("B004", "Earphone Sony WH-1000XM5", "Aksesoris",  6500000, 20, "1 Tahun")
            };
            for (Barang b : initial) { inventory.add(b); DBHelper.addBarang(b); }
        }

        riwayatTransaksi = DBHelper.loadRiwayat();
        transaksiCounter  = getNextTransaksiCounter();
    }

    private static int getNextTransaksiCounter() {
        String sql = "SELECT MAX(CAST(SUBSTRING(idPenjualan,5) AS UNSIGNED)) FROM penjualan";
        try (Connection conn = DBHelper.getConnection();
             Statement   stmt = conn.createStatement();
             ResultSet   rs   = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1) + 1;
        } catch (SQLException e) { e.printStackTrace(); }
        return 1;
    }

    // ================================================================
    //  LOGIN
    // ================================================================

    private static User loginGUI() {
        while (true) {
            JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
            JTextField    userField = new JTextField(15);
            JPasswordField passField = new JPasswordField(15);
            panel.add(new JLabel("Username :")); panel.add(userField);
            panel.add(new JLabel("Password :")); panel.add(passField);

            int opt = JOptionPane.showConfirmDialog(null, panel,
                    "Login Toko Elektronik", JOptionPane.OK_CANCEL_OPTION);
            if (opt != JOptionPane.OK_OPTION) return null;

            String username = userField.getText().trim();
            String password = new String(passField.getPassword()).trim();

            for (User u : users)
                if (u.login(username, password)) {
                    JOptionPane.showMessageDialog(null, "Login BERHASIL!\nSelamat datang, " + u.getRole());
                    return u;
                }
            JOptionPane.showMessageDialog(null, "Username atau password SALAH!", "Gagal", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ================================================================
    //  MAIN WINDOW
    // ================================================================

    private static JMenuItem styledMenu(String text) {
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
        JMenu    menu    = new JMenu("Fitur Program");

        JMenuItem itemStok = styledMenu("1. Cek Stok & Harga");
        itemStok.addActionListener(e -> cekStokDanHargaGUI(mainFrame));
        menu.add(itemStok);

        if (userLogin.getRole().equals("ADMIN")) {
            JMenuItem itemManaj = styledMenu("2. Manajemen Barang");
            itemManaj.addActionListener(e -> manajemenBarangGUI(mainFrame));
            menu.add(itemManaj);
        } else {
            JMenuItem itemTrans = styledMenu("2. Transaksi Penjualan (Kasir)");
            itemTrans.addActionListener(e -> transaksiPenjualanGUI(mainFrame));
            menu.add(itemTrans);
        }

        JMenuItem itemRiwayat = styledMenu("3. Riwayat Transaksi");
        itemRiwayat.addActionListener(e -> riwayatTransaksiGUI(mainFrame, userLogin));
        menu.add(itemRiwayat);

        JMenuItem itemLaporan = styledMenu("4. Laporan Stok Rendah");
        itemLaporan.addActionListener(e -> laporanStokRendahGUI(mainFrame));
        menu.add(itemLaporan);

        JMenuItem itemKeluar = styledMenu("5. Keluar");
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
            User u = loginGUI();
            if (u != null) createMainUI(u);
        });
    }

    // ================================================================
    //  TRANSAKSI PENJUALAN — JDialog proper, tidak pakai while+JOptionPane
    //
    //  FIX: Sebelumnya menggunakan while loop + JOptionPane berantai di EDT
    //       yang menyebabkan program freeze / tidak responsif / background
    //       terpotong saat di-stretch.
    //
    //  BARU: Seluruh UI transaksi ada dalam satu JDialog dengan:
    //    - Panel kiri : daftar barang (pilih via JComboBox + JSpinner)
    //    - Panel kanan: keranjang (JTable yang bisa diedit langsung)
    //    - Fitur edit keranjang: ubah jumlah atau hapus item
    //    - Tombol "Selesai & Bayar" hanya proses sekali, tidak loop
    // ================================================================

    private static void transaksiPenjualanGUI(JFrame parent) {

        // ---- Pilih customer dulu sebelum membuka dialog utama ----
        List<String> custNames = DBHelper.loadCustomerNames();
        JComboBox<String> comboCust = new JComboBox<>();
        comboCust.addItem("--- Pilih Customer ---");
        for (String n : custNames) comboCust.addItem(n);
        comboCust.addItem("Tambah Customer Baru");

        int r = JOptionPane.showConfirmDialog(parent, comboCust,
                "Pilih atau Tambah Customer", JOptionPane.OK_CANCEL_OPTION);
        if (r != JOptionPane.OK_OPTION) return;

        String namaCust = (String) comboCust.getSelectedItem();
        if (namaCust == null || namaCust.equals("--- Pilih Customer ---")) return;

        final String[] customerRef = new String[2]; // [0]=id, [1]=nama

        if (namaCust.equals("Tambah Customer Baru")) {
            String input = JOptionPane.showInputDialog(parent, "Nama Customer Baru:");
            if (input == null || input.trim().isEmpty()) return;
            customerRef[1] = input.trim();
            customerRef[0] = getOrCreateCustomerId(customerRef[1]);
            if (customerRef[0] == null) {
                JOptionPane.showMessageDialog(parent, "Gagal menambahkan customer.");
                return;
            }
        } else {
            customerRef[1] = namaCust;
            customerRef[0] = getCustomerIdByName(namaCust);
            if (customerRef[0] == null) {
                JOptionPane.showMessageDialog(parent, "ID customer tidak ditemukan.");
                return;
            }
        }

        // ---- Model untuk keranjang ----
        String[] cartCols = {"ID Barang", "Nama Barang", "Harga Satuan", "Jumlah", "Subtotal"};
        DefaultTableModel cartModel = new DefaultTableModel(cartCols, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        // cart item: [Barang barang, int jumlah]
        List<Object[]> cartItems = new ArrayList<>();

        // ---- Hitung total helper (dipanggil tiap perubahan keranjang) ----
        // Menggunakan array 1-elemen agar bisa diakses dari dalam lambda
        JLabel[] totalLabel = { new JLabel("Total: Rp 0") };
        totalLabel[0].setFont(new Font("Arial", Font.BOLD, 14));

        Runnable refreshTotal = () -> {
            double sub = 0;
            for (Object[] item : cartItems) {
                Barang b   = (Barang) item[0];
                int    qty = (int)    item[1];
                sub += b.getHarga() * qty;
            }
            double pajak = sub * 0.10;
            totalLabel[0].setText(String.format(
                    "<html>Subtotal: <b>Rp %,.0f</b> &nbsp;|&nbsp; Pajak 10%%: <b>Rp %,.0f</b> &nbsp;|&nbsp; TOTAL: <b>Rp %,.0f</b></html>",
                    sub, pajak, sub + pajak));
        };

        // ---- Dialog utama ----
        JDialog dialog = new JDialog(parent, "Transaksi Penjualan — " + customerRef[1], true);
        dialog.setSize(820, 580);
        dialog.setLocationRelativeTo(parent);
        dialog.setLayout(new BorderLayout(8, 8));

        // Panel atas: pilih barang
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        topPanel.setBorder(BorderFactory.createTitledBorder("Pilih Barang"));

        // ComboBox barang (diisi saat dialog dibuka, di-refresh tiap tambah)
        JComboBox<String> comboBarang = new JComboBox<>();
        comboBarang.setPreferredSize(new Dimension(380, 28));

        Runnable refreshCombo = () -> {
            comboBarang.removeAllItems();
            for (Barang b : inventory) {
                int diKeranjang = 0;
                for (Object[] item : cartItems)
                    if (((Barang) item[0]).getIdBarang().equals(b.getIdBarang()))
                        diKeranjang = (int) item[1];
                int sisa = b.getStok() - diKeranjang;
                if (sisa > 0)
                    comboBarang.addItem(String.format("%s  |  %s  |  Rp %,.0f  |  Stok: %d",
                            b.getIdBarang(), b.getNamaBarang(), b.getHarga(), sisa));
            }
        };
        refreshCombo.run();

        JSpinner spinJumlah = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
        spinJumlah.setPreferredSize(new Dimension(70, 28));

        // Update spinner max saat item combo berubah
        comboBarang.addActionListener(e -> {
            int idx = comboBarang.getSelectedIndex();
            if (idx < 0) return;
            List<Barang> tersedia = getTersedia(cartItems);
            if (idx >= tersedia.size()) return;
            Barang b = tersedia.get(idx);
            int diKeranjang = 0;
            for (Object[] item : cartItems)
                if (((Barang) item[0]).getIdBarang().equals(b.getIdBarang()))
                    diKeranjang = (int) item[1];
            int sisa = b.getStok() - diKeranjang;
            ((SpinnerNumberModel) spinJumlah.getModel()).setMaximum(sisa);
            ((SpinnerNumberModel) spinJumlah.getModel()).setValue(1);
        });

        JButton btnTambah = new JButton("+ Tambah ke Keranjang");
        btnTambah.addActionListener(e -> {
            int idx = comboBarang.getSelectedIndex();
            if (idx < 0) { JOptionPane.showMessageDialog(dialog, "Pilih barang dulu!"); return; }

            List<Barang> tersedia = getTersedia(cartItems);
            if (idx >= tersedia.size()) return;
            Barang barangPilih = tersedia.get(idx);
            int qty = (int) spinJumlah.getValue();

            // Jika barang sudah di keranjang, tambah jumlahnya
            boolean found = false;
            for (Object[] item : cartItems) {
                if (((Barang) item[0]).getIdBarang().equals(barangPilih.getIdBarang())) {
                    int newQty = (int) item[1] + qty;
                    int stokSisa = barangPilih.getStok();
                    if (newQty > stokSisa) {
                        JOptionPane.showMessageDialog(dialog, "Jumlah melebihi stok tersedia!");
                        return;
                    }
                    item[1] = newQty;
                    // Update baris di tabel
                    for (int row = 0; row < cartModel.getRowCount(); row++) {
                        if (cartModel.getValueAt(row, 0).equals(barangPilih.getIdBarang())) {
                            cartModel.setValueAt(newQty, row, 3);
                            cartModel.setValueAt(String.format("Rp %,.0f", barangPilih.getHarga() * newQty), row, 4);
                            break;
                        }
                    }
                    found = true;
                    break;
                }
            }

            if (!found) {
                cartItems.add(new Object[]{barangPilih, qty});
                cartModel.addRow(new Object[]{
                        barangPilih.getIdBarang(),
                        barangPilih.getNamaBarang(),
                        String.format("Rp %,.0f", barangPilih.getHarga()),
                        qty,
                        String.format("Rp %,.0f", barangPilih.getHarga() * qty)
                });
            }

            refreshCombo.run();
            refreshTotal.run();
        });

        topPanel.add(new JLabel("Barang:"));
        topPanel.add(comboBarang);
        topPanel.add(new JLabel("Jumlah:"));
        topPanel.add(spinJumlah);
        topPanel.add(btnTambah);

        // Panel tengah: keranjang
        JTable cartTable = new JTable(cartModel);
        cartTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        cartTable.setRowHeight(24);
        JScrollPane cartScroll = new JScrollPane(cartTable);
        cartScroll.setBorder(BorderFactory.createTitledBorder("Keranjang Belanja"));

        // Panel edit keranjang
        JPanel editPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));

        JButton btnEdit = new JButton("✏️ Edit Jumlah");
        btnEdit.addActionListener(e -> {
            int row = cartTable.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(dialog, "Pilih item di keranjang dulu!"); return; }

            Object[] item     = cartItems.get(row);
            Barang   barang   = (Barang) item[0];
            int      qtyLama  = (int) item[1];
            int      maxStok  = barang.getStok(); // stok total di inventory

            SpinnerNumberModel sm = new SpinnerNumberModel(qtyLama, 1, maxStok, 1);
            JSpinner sp = new JSpinner(sm);

            int res = JOptionPane.showConfirmDialog(dialog,
                    new Object[]{"Jumlah baru untuk " + barang.getNamaBarang() + " (maks. " + maxStok + "):", sp},
                    "Edit Jumlah", JOptionPane.OK_CANCEL_OPTION);
            if (res != JOptionPane.OK_OPTION) return;

            int qtyBaru = (int) sp.getValue();
            item[1] = qtyBaru;
            cartModel.setValueAt(qtyBaru, row, 3);
            cartModel.setValueAt(String.format("Rp %,.0f", barang.getHarga() * qtyBaru), row, 4);

            refreshCombo.run();
            refreshTotal.run();
        });

        JButton btnHapusItem = new JButton("🗑️ Hapus Item");
        btnHapusItem.addActionListener(e -> {
            int row = cartTable.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(dialog, "Pilih item di keranjang dulu!"); return; }

            int confirm = JOptionPane.showConfirmDialog(dialog,
                    "Hapus item ini dari keranjang?", "Konfirmasi", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;

            cartItems.remove(row);
            cartModel.removeRow(row);
            refreshCombo.run();
            refreshTotal.run();
        });

        editPanel.add(btnEdit);
        editPanel.add(btnHapusItem);

        // Panel bawah: total + tombol aksi
        JPanel bottomPanel = new JPanel(new BorderLayout(8, 4));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));

        JPanel totalPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        totalPanel.add(totalLabel[0]);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton btnBayar = new JButton("✅ Selesai & Bayar");
        JButton btnBatal = new JButton("❌ Batal");

        btnBayar.setBackground(new Color(0, 153, 76));
        btnBayar.setForeground(Color.WHITE);
        btnBayar.setFont(new Font("Arial", Font.BOLD, 13));
        btnBatal.setFont(new Font("Arial", Font.BOLD, 13));

        // FIX: Proses pembayaran dijalankan TANPA menutup dialog dulu,
        // sehingga tidak ada window yang hilang tiba-tiba lalu freeze.
        btnBayar.addActionListener(e -> {
            if (cartItems.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Keranjang masih kosong!");
                return;
            }

            // Buat objek Penjualan dari keranjang
            List<PenjualanDetail> detailList = new ArrayList<>();
            int detailIdx = 1;
            for (Object[] item : cartItems) {
                Barang b   = (Barang) item[0];
                int    qty = (int)    item[1];
                String idDetail = "DP-" + String.format("%04d", transaksiCounter)
                        + "-" + String.format("%02d", detailIdx++);
                PenjualanDetail d = new PenjualanDetail(idDetail, b.getIdBarang(), qty);
                d.hitungSubtotal(b.getHarga());
                detailList.add(d);
            }

            String idTrans = "TRX-" + String.format("%04d", transaksiCounter);
            Penjualan transaksi = new Penjualan(idTrans, customerRef[0], customerRef[1], detailList);
            transaksi.hitungTotal();

            // Konfirmasi sebelum simpan
            int confirm = JOptionPane.showConfirmDialog(dialog,
                    "Simpan transaksi " + idTrans + "?\n"
                    + String.format("TOTAL BAYAR: Rp %,.0f", transaksi.getTotalAkhir()),
                    "Konfirmasi Pembayaran", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;

            boolean sukses = simpanTransaksiKeDatabase(transaksi);
            if (sukses) {
                transaksiCounter++;
                riwayatTransaksi.add(transaksi);

                // Tampilkan struk SETELAH dialog transaksi tutup
                dialog.dispose();

                String struk = transaksi.getStrukText(inventory.toArray(new Barang[0]));
                JTextArea ta = new JTextArea(struk);
                ta.setEditable(false);
                ta.setFont(new Font("Monospaced", Font.PLAIN, 13));
                JOptionPane.showMessageDialog(parent, new JScrollPane(ta),
                        "STRUK PENJUALAN", JOptionPane.INFORMATION_MESSAGE);
                JOptionPane.showMessageDialog(parent, "✅ Transaksi berhasil disimpan!");
            } else {
                JOptionPane.showMessageDialog(dialog, "❌ Transaksi gagal disimpan.");
            }
        });

        btnBatal.addActionListener(e -> {
            if (!cartItems.isEmpty()) {
                int confirm = JOptionPane.showConfirmDialog(dialog,
                        "Keranjang tidak kosong. Yakin ingin membatalkan transaksi?",
                        "Konfirmasi Batal", JOptionPane.YES_NO_OPTION);
                if (confirm != JOptionPane.YES_OPTION) return;
            }
            dialog.dispose();
        });

        btnPanel.add(btnBayar);
        btnPanel.add(btnBatal);

        bottomPanel.add(totalPanel, BorderLayout.CENTER);
        bottomPanel.add(btnPanel,   BorderLayout.EAST);

        // Susun dialog
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(cartScroll, BorderLayout.CENTER);
        centerPanel.add(editPanel,  BorderLayout.SOUTH);

        dialog.add(topPanel,     BorderLayout.NORTH);
        dialog.add(centerPanel,  BorderLayout.CENTER);
        dialog.add(bottomPanel,  BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    /** Ambil daftar barang yang masih ada stok sisa (stok - qty di keranjang > 0) */
    private static List<Barang> getTersedia(List<Object[]> cartItems) {
        List<Barang> list = new ArrayList<>();
        for (Barang b : inventory) {
            int diKeranjang = 0;
            for (Object[] item : cartItems)
                if (((Barang) item[0]).getIdBarang().equals(b.getIdBarang()))
                    diKeranjang = (int) item[1];
            if (b.getStok() - diKeranjang > 0) list.add(b);
        }
        return list;
    }

    private static boolean simpanTransaksiKeDatabase(Penjualan transaksi) {
        Connection conn = null;
        try {
            conn = DBHelper.getConnection();
            conn.setAutoCommit(false);

            Map<String, Integer> qtyMap = new HashMap<>();
            for (PenjualanDetail d : transaksi.getDetailList())
                qtyMap.put(d.getIdBarang(), qtyMap.getOrDefault(d.getIdBarang(), 0) + d.getJumlah());

            for (Map.Entry<String, Integer> en : qtyMap.entrySet()) {
                Barang b = findBarangById(en.getKey());
                if (b == null) throw new SQLException("Barang tidak ditemukan: " + en.getKey());
                if (b.getStok() < en.getValue()) throw new SQLException("Stok tidak cukup: " + b.getNamaBarang());
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO penjualan(idPenjualan,tanggal,idCustomer,namaCustomer,subtotal,pajak,totalAkhir) VALUES(?,?,?,?,?,?,?)")) {
                ps.setString(1, transaksi.getIdPenjualan());
                ps.setTimestamp(2, new Timestamp(transaksi.getTanggal().getTime()));
                ps.setString(3, transaksi.getIdCustomer());
                ps.setString(4, transaksi.getNamaCustomer());
                ps.setDouble(5, transaksi.getSubtotal());
                ps.setDouble(6, transaksi.getPajak());
                ps.setDouble(7, transaksi.getTotalAkhir());
                ps.executeUpdate();
            }

            try (PreparedStatement pd = conn.prepareStatement(
                    "INSERT INTO detail_penjualan(idDetailPenjualan,idPenjualan,idBarang,jumlah,subtotal) VALUES(?,?,?,?,?)");
                 PreparedStatement pk = conn.prepareStatement(
                    "UPDATE barang SET stok = stok - ? WHERE idBarang = ?")) {
                for (PenjualanDetail d : transaksi.getDetailList()) {
                    pd.setString(1, d.getIdDetailPenjualan());
                    pd.setString(2, transaksi.getIdPenjualan());
                    pd.setString(3, d.getIdBarang());
                    pd.setInt(4, d.getJumlah());
                    pd.setDouble(5, d.getSubtotal());
                    pd.executeUpdate();
                }
                for (Map.Entry<String, Integer> en : qtyMap.entrySet()) {
                    pk.setInt(1, en.getValue()); pk.setString(2, en.getKey()); pk.executeUpdate();
                }
            }

            conn.commit();
            for (Map.Entry<String, Integer> en : qtyMap.entrySet()) {
                Barang b = findBarangById(en.getKey());
                if (b != null) b.setStok(b.getStok() - en.getValue());
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

    // ================================================================
    //  RIWAYAT TRANSAKSI — Edit & Hapus HANYA untuk ADMIN
    // ================================================================

    private static void riwayatTransaksiGUI(JFrame parent, User user) {
        if (riwayatTransaksi.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Belum ada transaksi.");
            return;
        }

        boolean isAdmin = user.getRole().equals("ADMIN");

        String[] col = {"ID Transaksi", "Tanggal", "Customer", "Total"};
        DefaultTableModel model = new DefaultTableModel(col, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (Penjualan p : riwayatTransaksi)
            model.addRow(new Object[]{
                    p.getIdPenjualan(), p.getTanggal(),
                    p.getNamaCustomer(), String.format("Rp %,.0f", p.getTotalAkhir())
            });

        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);
        JScrollPane scroll = new JScrollPane(table);

        JDialog dialog = new JDialog(parent, "Riwayat Transaksi", true);
        dialog.setLayout(new BorderLayout());
        dialog.add(scroll, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));

        if (isAdmin) {
            JButton btnEdit  = new JButton("✏️ Edit Transaksi");
            JButton btnHapus = new JButton("🗑️ Hapus Transaksi");

            btnEdit.addActionListener(e -> {
                int row = table.getSelectedRow();
                if (row < 0) { JOptionPane.showMessageDialog(dialog, "Pilih transaksi dulu!"); return; }
                String id = (String) model.getValueAt(row, 0);
                if (editTransaksiById(id, dialog))
                    for (Penjualan p : riwayatTransaksi)
                        if (p.getIdPenjualan().equals(id)) { model.setValueAt(p.getNamaCustomer(), row, 2); break; }
            });

            btnHapus.addActionListener(e -> {
                int row = table.getSelectedRow();
                if (row < 0) { JOptionPane.showMessageDialog(dialog, "Pilih transaksi dulu!"); return; }
                String id = (String) model.getValueAt(row, 0);
                if (hapusTransaksiById(id, dialog)) model.removeRow(row);
            });

            btnPanel.add(btnEdit);
            btnPanel.add(btnHapus);
        } else {
            JLabel info = new JLabel("ℹ️  Mode Kasir: riwayat hanya dapat dilihat.");
            info.setForeground(new Color(180, 60, 0));
            info.setFont(new Font("Arial", Font.ITALIC, 12));
            btnPanel.add(info);
        }

        JButton btnTutup = new JButton("Tutup");
        btnTutup.addActionListener(e -> dialog.dispose());
        btnPanel.add(btnTutup);

        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setSize(850, 550);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    // ================================================================
    //  CEK STOK
    // ================================================================

    private static void cekStokDanHargaGUI(JFrame parent) {
        String[] col = {"ID", "Nama Barang", "Kategori", "Harga", "Stok"};
        Object[][] data = new Object[inventory.size()][5];
        for (int i = 0; i < inventory.size(); i++) {
            Barang b = inventory.get(i);
            data[i] = new Object[]{ b.getIdBarang(), b.getNamaBarang(),
                    b.getKategori(), String.format("Rp %,.0f", b.getHarga()), b.getStok() };
        }
        JTable table = new JTable(data, col);
        table.setRowHeight(24);
        JDialog dialog = new JDialog(parent, "Daftar Barang & Stok", true);
        dialog.setLayout(new BorderLayout());
        dialog.add(new JScrollPane(table), BorderLayout.CENTER);
        JButton tutup = new JButton("Tutup");
        tutup.addActionListener(e -> dialog.dispose());
        JPanel bp = new JPanel(); bp.add(tutup);
        dialog.add(bp, BorderLayout.SOUTH);
        dialog.setSize(650, 400);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    // ================================================================
    //  MANAJEMEN BARANG (ADMIN)
    // ================================================================

    private static void manajemenBarangGUI(JFrame parent) {
        String[] pilihan = {"Tambah Barang Baru", "Edit Harga / Stok", "Hapus Barang", "Kembali"};
        int p = JOptionPane.showOptionDialog(parent, "Pilih aksi:", "Manajemen Barang (ADMIN)",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, pilihan, pilihan[0]);
        if (p == 0) tambahBarangBaruGUI(parent);
        else if (p == 1) editBarangGUI(parent);
        else if (p == 2) hapusBarangGUI(parent);
    }

    private static void tambahBarangBaruGUI(JFrame parent) {
        JTextField idF = new JTextField(), namaF = new JTextField(),
                   katF = new JTextField(), hargaF = new JTextField(),
                   stokF = new JTextField(), garansiF = new JTextField();
        Object[] msg = {
                "ID Barang:", idF, "Nama Barang:", namaF, "Kategori:", katF,
                "Harga (Rp):", hargaF, "Stok Awal:", stokF, "Garansi (opsional):", garansiF
        };
        if (JOptionPane.showConfirmDialog(parent, msg, "Tambah Barang Baru",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

        String id = idF.getText().trim(), nama = namaF.getText().trim(), kat = katF.getText().trim();
        if (id.isEmpty() || nama.isEmpty() || kat.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "ID, Nama, dan Kategori wajib diisi!"); return;
        }
        for (Barang b : inventory) if (b.getIdBarang().equalsIgnoreCase(id)) {
            JOptionPane.showMessageDialog(parent, "❌ ID sudah ada!"); return;
        }
        try {
            double harga = Double.parseDouble(hargaF.getText().trim());
            int    stok  = Integer.parseInt(stokF.getText().trim());
            if (harga < 0 || stok < 0) { JOptionPane.showMessageDialog(parent, "Harga/stok tidak boleh negatif!"); return; }
            Barang baru = new Barang(id, nama, kat, harga, stok, garansiF.getText().trim());
            inventory.add(baru); DBHelper.addBarang(baru);
            JOptionPane.showMessageDialog(parent, "✅ Barang berhasil ditambahkan!");
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(parent, "Input harga/stok tidak valid!");
        }
    }

    private static void editBarangGUI(JFrame parent) {
        if (inventory.isEmpty()) { JOptionPane.showMessageDialog(parent, "Inventory kosong."); return; }

        String[] labels = new String[inventory.size()];
        for (int i = 0; i < inventory.size(); i++) {
            Barang b = inventory.get(i);
            labels[i] = String.format("%s  |  %s  |  Rp %,.0f  |  Stok: %d",
                    b.getIdBarang(), b.getNamaBarang(), b.getHarga(), b.getStok());
        }
        JComboBox<String> combo = new JComboBox<>(labels);
        combo.setPreferredSize(new Dimension(480, 28));

        if (JOptionPane.showConfirmDialog(parent, combo, "Pilih Barang yang Diedit",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

        Barang target = inventory.get(combo.getSelectedIndex());
        JTextField hargaF = new JTextField(String.valueOf(target.getHarga()));
        JTextField stokF  = new JTextField(String.valueOf(target.getStok()));
        Object[] msg = { "Barang: " + target.getNamaBarang(), "Harga baru (Rp):", hargaF, "Stok baru:", stokF };

        if (JOptionPane.showConfirmDialog(parent, msg, "Edit Barang",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try {
            double harga = Double.parseDouble(hargaF.getText().trim());
            int    stok  = Integer.parseInt(stokF.getText().trim());
            if (harga < 0 || stok < 0) { JOptionPane.showMessageDialog(parent, "Tidak boleh negatif!"); return; }
            target.setHarga(harga); target.setStok(stok);
            DBHelper.updateBarang(target.getIdBarang(), harga, stok);
            JOptionPane.showMessageDialog(parent, "✅ Barang berhasil diupdate!");
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(parent, "Input tidak valid!");
        }
    }

    private static void hapusBarangGUI(JFrame parent) {
        if (inventory.isEmpty()) { JOptionPane.showMessageDialog(parent, "Inventory kosong."); return; }

        List<Barang> bisaDihapus = new ArrayList<>();
        for (Barang b : inventory) if (!DBHelper.isBarangUsed(b.getIdBarang())) bisaDihapus.add(b);

        if (bisaDihapus.isEmpty()) {
            JOptionPane.showMessageDialog(parent,
                    "Semua barang sudah pernah dijual.\nTidak ada yang bisa dihapus."); return;
        }

        String[] labels = new String[bisaDihapus.size()];
        for (int i = 0; i < bisaDihapus.size(); i++) {
            Barang b = bisaDihapus.get(i);
            labels[i] = String.format("%s  |  %s  |  Stok: %d", b.getIdBarang(), b.getNamaBarang(), b.getStok());
        }
        JComboBox<String> combo = new JComboBox<>(labels);
        combo.setPreferredSize(new Dimension(420, 28));

        if (JOptionPane.showConfirmDialog(parent, combo, "Pilih Barang yang Dihapus",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

        Barang target = bisaDihapus.get(combo.getSelectedIndex());
        if (JOptionPane.showConfirmDialog(parent,
                "Hapus " + target.getNamaBarang() + "?", "Konfirmasi", JOptionPane.YES_NO_OPTION)
                != JOptionPane.YES_OPTION) return;

        inventory.remove(target);
        DBHelper.deleteBarang(target.getIdBarang());
        JOptionPane.showMessageDialog(parent, "✅ Barang berhasil dihapus!");
    }

    private static void laporanStokRendahGUI(JFrame parent) {
        StringBuilder sb = new StringBuilder("=== LAPORAN STOK RENDAH (< 5) ===\n\n");
        boolean ada = false;
        for (Barang b : inventory) if (b.getStok() < 5) {
            sb.append(String.format("%s - %s | Stok: %d\n", b.getIdBarang(), b.getNamaBarang(), b.getStok()));
            ada = true;
        }
        if (!ada) sb.append("Semua stok masih aman.");
        JTextArea ta = new JTextArea(sb.toString()); ta.setEditable(false);
        JOptionPane.showMessageDialog(parent, new JScrollPane(ta), "Laporan Stok Rendah", JOptionPane.PLAIN_MESSAGE);
    }

    // ================================================================
    //  EDIT & HAPUS TRANSAKSI (ADMIN only)
    // ================================================================

    private static boolean editTransaksiById(String id, Component parent) {
        for (Penjualan p : riwayatTransaksi) if (p.getIdPenjualan().equals(id)) {
            String namaBaru = JOptionPane.showInputDialog(parent, "Nama Customer baru:", p.getNamaCustomer());
            if (namaBaru != null && !namaBaru.trim().isEmpty()) {
                namaBaru = namaBaru.trim();
                p.setNamaCustomer(namaBaru);
                DBHelper.updateNamaCustomer(id, namaBaru);
                JOptionPane.showMessageDialog(parent, "✅ Nama customer diupdate!");
                return true;
            }
            return false;
        }
        JOptionPane.showMessageDialog(parent, "Transaksi tidak ditemukan!", "Error", JOptionPane.ERROR_MESSAGE);
        return false;
    }

    private static boolean hapusTransaksiById(String id, Component parent) {
        Penjualan target = null;
        for (Penjualan p : riwayatTransaksi) if (p.getIdPenjualan().equals(id)) { target = p; break; }
        if (target == null) {
            JOptionPane.showMessageDialog(parent, "Transaksi tidak ditemukan!", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (JOptionPane.showConfirmDialog(parent,
                "Hapus " + id + "?\nStok barang akan dikembalikan.",
                "Konfirmasi Hapus", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return false;

        Map<String, Integer> qtyMap = new HashMap<>();
        for (PenjualanDetail d : target.getDetailList())
            qtyMap.put(d.getIdBarang(), qtyMap.getOrDefault(d.getIdBarang(), 0) + d.getJumlah());

        Connection conn = null;
        try {
            conn = DBHelper.getConnection(); conn.setAutoCommit(false);
            try (PreparedStatement p1 = conn.prepareStatement("DELETE FROM detail_penjualan WHERE idPenjualan=?")) {
                p1.setString(1, id); p1.executeUpdate(); }
            try (PreparedStatement p2 = conn.prepareStatement("DELETE FROM penjualan WHERE idPenjualan=?")) {
                p2.setString(1, id); p2.executeUpdate(); }
            try (PreparedStatement p3 = conn.prepareStatement("UPDATE barang SET stok=stok+? WHERE idBarang=?")) {
                for (Map.Entry<String, Integer> en : qtyMap.entrySet()) {
                    p3.setInt(1, en.getValue()); p3.setString(2, en.getKey()); p3.executeUpdate(); }
            }
            conn.commit();
            for (Map.Entry<String, Integer> en : qtyMap.entrySet()) {
                Barang b = findBarangById(en.getKey());
                if (b != null) b.setStok(b.getStok() + en.getValue());
            }
            riwayatTransaksi.remove(target);
            JOptionPane.showMessageDialog(parent, "✅ Transaksi dihapus, stok dikembalikan!");
            return true;
        } catch (Exception e) {
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
            JOptionPane.showMessageDialog(parent, "❌ Gagal menghapus transaksi.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        } finally {
            try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    // ================================================================
    //  HELPER
    // ================================================================

    private static Barang findBarangById(String id) {
        for (Barang b : inventory) if (b.getIdBarang().equalsIgnoreCase(id)) return b;
        return null;
    }

    private static String getCustomerIdByName(String nama) {
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT idCustomer FROM customer WHERE nama=? ORDER BY idCustomer DESC LIMIT 1")) {
            ps.setString(1, nama);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getString("idCustomer"); }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    private static String getOrCreateCustomerId(String nama) {
        String id = getCustomerIdByName(nama);
        if (id != null) return id;
        id = "CUST-" + System.currentTimeMillis();
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO customer(idCustomer,nama,telepon,alamat) VALUES(?,?,?,?)")) {
            ps.setString(1, id); ps.setString(2, nama); ps.setString(3, "-"); ps.setString(4, "-");
            ps.executeUpdate();
            return id;
        } catch (SQLException e) { e.printStackTrace(); return null; }
    }
}

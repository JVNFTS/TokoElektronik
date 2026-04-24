public class Barang {
    private String idBarang;
    private String namaBarang;
    private String kategori;
    private double harga;
    private int stok;
    private String garansi;

    public Barang(String idBarang, String namaBarang, String kategori, double harga, int stok, String garansi) {
        if (idBarang == null || idBarang.trim().isEmpty()) {
            throw new IllegalArgumentException("ID barang tidak boleh kosong");
        }
        if (namaBarang == null || namaBarang.trim().isEmpty()) {
            throw new IllegalArgumentException("Nama barang tidak boleh kosong");
        }
        if (kategori == null || kategori.trim().isEmpty()) {
            throw new IllegalArgumentException("Kategori tidak boleh kosong");
        }
        if (harga < 0) {
            throw new IllegalArgumentException("Harga tidak boleh negatif");
        }
        if (stok < 0) {
            throw new IllegalArgumentException("Stok tidak boleh negatif");
        }

        this.idBarang = idBarang.trim();
        this.namaBarang = namaBarang.trim();
        this.kategori = kategori.trim();
        this.harga = harga;
        this.stok = stok;
        // [FIX] garansi null → string kosong (bukan throw), karena garansi memang opsional
        this.garansi = (garansi != null) ? garansi.trim() : "";
    }

    // ================== Getters ==================

    public String getIdBarang() {
        return idBarang;
    }

    public String getNamaBarang() {
        return namaBarang;
    }

    public String getKategori() {
        return kategori;
    }

    public double getHarga() {
        return harga;
    }

    public int getStok() {
        return stok;
    }

    public String getGaransi() {
        return garansi;
    }

    // ================== Setters ==================

    public void setHarga(double harga) {
        if (harga < 0) {
            throw new IllegalArgumentException("Harga tidak boleh negatif");
        }
        this.harga = harga;
    }

    public void setStok(int stok) {
        if (stok < 0) {
            throw new IllegalArgumentException("Stok tidak boleh negatif");
        }
        this.stok = stok;
    }

    public void setNamaBarang(String namaBarang) {
        if (namaBarang == null || namaBarang.trim().isEmpty()) {
            throw new IllegalArgumentException("Nama barang tidak boleh kosong");
        }
        this.namaBarang = namaBarang.trim();
    }

    public void setKategori(String kategori) {
        if (kategori == null || kategori.trim().isEmpty()) {
            throw new IllegalArgumentException("Kategori tidak boleh kosong");
        }
        this.kategori = kategori.trim();
    }

    public void setGaransi(String garansi) {
        this.garansi = (garansi != null) ? garansi.trim() : "";
    }

    /**
     * Representasi string untuk debugging.
     */
    @Override
    public String toString() {
        return String.format("[%s] %s | %s | Rp %.0f | Stok: %d", idBarang, namaBarang, kategori, harga, stok);
    }
}
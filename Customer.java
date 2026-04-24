public class Customer {
    private String idCustomer;
    private String nama;
    private String telepon;
    private String alamat;

    public Customer(String idCustomer, String nama, String telepon, String alamat) {
        if (idCustomer == null || idCustomer.trim().isEmpty()) {
            throw new IllegalArgumentException("ID customer tidak boleh kosong");
        }
        if (nama == null || nama.trim().isEmpty()) {
            throw new IllegalArgumentException("Nama customer tidak boleh kosong");
        }

        this.idCustomer = idCustomer.trim();
        this.nama = nama.trim();
        // telepon & alamat opsional: null atau kosong → default "-"
        this.telepon = (telepon != null && !telepon.trim().isEmpty()) ? telepon.trim() : "-";
        this.alamat = (alamat != null && !alamat.trim().isEmpty()) ? alamat.trim() : "-";
    }

    // ================== Getters ==================

    public String getIdCustomer() {
        return idCustomer;
    }

    public String getNama() {
        return nama;
    }

    public String getTelepon() {
        return telepon;
    }

    public String getAlamat() {
        return alamat;
    }

    // ================== Setters ==================

    public void setNama(String nama) {
        if (nama == null || nama.trim().isEmpty()) {
            throw new IllegalArgumentException("Nama customer tidak boleh kosong");
        }
        this.nama = nama.trim();
    }

    public void setTelepon(String telepon) {
        this.telepon = (telepon != null && !telepon.trim().isEmpty()) ? telepon.trim() : "-";
    }

    public void setAlamat(String alamat) {
        this.alamat = (alamat != null && !alamat.trim().isEmpty()) ? alamat.trim() : "-";
    }

    /**
     * Representasi string untuk debugging.
     */
    @Override
    public String toString() {
        return String.format("[%s] %s | %s | %s", idCustomer, nama, telepon, alamat);
    }
}
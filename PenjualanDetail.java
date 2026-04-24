public class PenjualanDetail {
    private String idDetailPenjualan;
    private String idBarang;
    private int jumlah;
    private double subtotal;

    // Constructor untuk transaksi baru
    public PenjualanDetail(String idDetailPenjualan, String idBarang, int jumlah) {
        this.idDetailPenjualan = idDetailPenjualan;
        this.idBarang = idBarang;
        setJumlah(jumlah);
        this.subtotal = 0.0;
    }

    // Constructor untuk load dari database
    public PenjualanDetail(String idDetailPenjualan, String idBarang, int jumlah, double subtotal) {
        this.idDetailPenjualan = idDetailPenjualan;
        this.idBarang = idBarang;
        setJumlah(jumlah);
        setSubtotal(subtotal);
    }

    public void hitungSubtotal(double hargaBarang) {
        if (hargaBarang < 0) {
            throw new IllegalArgumentException("Harga barang tidak boleh negatif");
        }
        this.subtotal = hargaBarang * jumlah;
    }

    public String getIdDetailPenjualan() {
        return idDetailPenjualan;
    }

    public String getIdBarang() {
        return idBarang;
    }

    public int getJumlah() {
        return jumlah;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setJumlah(int jumlah) {
        if (jumlah <= 0) {
            throw new IllegalArgumentException("Jumlah harus lebih dari 0");
        }
        this.jumlah = jumlah;
    }

    public void setSubtotal(double subtotal) {
        if (subtotal < 0) {
            throw new IllegalArgumentException("Subtotal tidak boleh negatif");
        }
        this.subtotal = subtotal;
    }
}
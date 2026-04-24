import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class Penjualan {
    private String idPenjualan;
    private Date tanggal;
    private String idCustomer;
    private String namaCustomer;
    private double subtotal;
    private double pajak;
    private double totalAkhir;
    private List<PenjualanDetail> detailList;

    private static final double PAJAK_RATE = 0.10; // 10%

    /**
     * Constructor untuk transaksi baru.
     * [FIX] hitungTotal() sekarang dipanggil otomatis agar subtotal/pajak/totalAkhir
     * langsung terisi, konsisten dengan constructor load-dari-database.
     * Sebelumnya: nilai total = 0 sampai pemanggil secara manual memanggil hitungTotal().
     */
    public Penjualan(String idPenjualan, String idCustomer, String namaCustomer, List<PenjualanDetail> detailList) {
        this.idPenjualan = idPenjualan;
        this.tanggal = new Date();
        this.idCustomer = idCustomer;
        this.namaCustomer = (namaCustomer != null) ? namaCustomer.trim() : "";
        this.detailList = (detailList != null) ? detailList : new ArrayList<>();
        hitungTotal(); // [FIX] Konsisten dengan constructor DB
    }

    /**
     * Constructor untuk load dari database.
     */
    public Penjualan(String idPenjualan, Date tanggal, String idCustomer, String namaCustomer, List<PenjualanDetail> detailList) {
        this.idPenjualan = idPenjualan;
        this.tanggal = tanggal != null ? new Date(tanggal.getTime()) : new Date();
        this.idCustomer = idCustomer;
        this.namaCustomer = (namaCustomer != null) ? namaCustomer.trim() : "";
        this.detailList = (detailList != null) ? detailList : new ArrayList<>();
        hitungTotal();
    }

    public void hitungTotal() {
        subtotal = 0.0;

        if (detailList != null) {
            for (PenjualanDetail d : detailList) {
                if (d != null) {
                    subtotal += d.getSubtotal();
                }
            }
        }

        pajak = subtotal * PAJAK_RATE;
        totalAkhir = subtotal + pajak;
    }

    public String getStrukText(Barang[] barangList) {
        StringBuilder sb = new StringBuilder();

        sb.append("\n=====================================\n");
        sb.append("           STRUK PENJUALAN           \n");
        sb.append("=====================================\n");
        sb.append("ID Transaksi   : ").append(idPenjualan).append("\n");
        sb.append("Tanggal        : ").append(tanggal).append("\n");
        sb.append("Customer       : ").append(namaCustomer).append("\n");
        sb.append("-------------------------------------\n");

        if (detailList != null) {
            for (PenjualanDetail d : detailList) {
                if (d == null) continue;

                Barang barang = cariBarang(barangList, d.getIdBarang());
                String namaBarang = (barang != null) ? barang.getNamaBarang() : "Barang Tidak Ditemukan";
                double hargaBarang = (barang != null) ? barang.getHarga() : 0;

                sb.append(String.format(
                        "%s x %d @ Rp %.0f = Rp %.0f\n",
                        namaBarang,
                        d.getJumlah(),
                        hargaBarang,
                        d.getSubtotal()
                ));
            }
        }

        sb.append("-------------------------------------\n");
        sb.append(String.format("Subtotal       : Rp %.0f\n", subtotal));
        sb.append(String.format("Pajak (10%%)    : Rp %.0f\n", pajak));
        sb.append(String.format("TOTAL BAYAR    : Rp %.0f\n", totalAkhir));
        sb.append("=====================================\n");

        return sb.toString();
    }

    private Barang cariBarang(Barang[] barangList, String idBarang) {
        if (barangList == null || idBarang == null) return null;

        for (Barang b : barangList) {
            if (b != null && idBarang.equalsIgnoreCase(b.getIdBarang())) {
                return b;
            }
        }
        return null;
    }

    public String getIdPenjualan() {
        return idPenjualan;
    }

    /**
     * [FIX] Kembalikan defensive copy agar objek Date internal tidak bisa
     * dimodifikasi dari luar class.
     */
    public Date getTanggal() {
        return new Date(tanggal.getTime());
    }

    public String getIdCustomer() {
        return idCustomer;
    }

    public String getNamaCustomer() {
        return namaCustomer;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public double getPajak() {
        return pajak;
    }

    public double getTotalAkhir() {
        return totalAkhir;
    }

    /**
     * [FIX] Kembalikan unmodifiable view agar list internal tidak bisa
     * diubah langsung dari luar (misalnya dengan .add() atau .remove()).
     * Kode lain yang hanya membaca list tidak terpengaruh.
     */
    public List<PenjualanDetail> getDetailList() {
        return Collections.unmodifiableList(detailList);
    }

    public void setNamaCustomer(String namaCustomer) {
        if (namaCustomer != null && !namaCustomer.trim().isEmpty()) {
            this.namaCustomer = namaCustomer.trim();
        }
    }
}
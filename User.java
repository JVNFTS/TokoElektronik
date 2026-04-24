public class User {
    private String username;
    private String password;
    private String role;

    public User(String username, String password, String role) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username tidak boleh kosong");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password tidak boleh kosong");
        }
        if (role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("Role tidak boleh kosong");
        }

        this.username = username.trim();
        this.password = password.trim(); // password disimpan sudah di-trim
        this.role = role.trim().toUpperCase();
    }

    /**
     * [FIX] passwordInput sekarang ikut di-trim sebelum dibandingkan,
     * konsisten dengan cara password disimpan di constructor (juga di-trim).
     * Sebelumnya: "123 " (ada spasi) tidak cocok dengan "123" → login gagal.
     */
    public boolean login(String usernameInput, String passwordInput) {
        if (usernameInput == null || passwordInput == null) {
            return false;
        }
        return this.username.equals(usernameInput.trim())
                && this.password.equals(passwordInput.trim());
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getRole() {
        return role;
    }

    public void setPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password tidak boleh kosong");
        }
        this.password = password.trim();
    }

    public void setRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("Role tidak boleh kosong");
        }
        this.role = role.trim().toUpperCase();
    }
}
package chatServer;

import dependencies.functions.SecureHash;
import dependencies.lib.STATUS;
import dependencies.lib.UserBean;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.*;

public class UserDao {

    public static UserBean login(UserBean bean, String db_username, String db_password) throws SQLException,
            ClassNotFoundException, InvalidKeySpecException, NoSuchAlgorithmException {
        Connection con = ConnectionManager.getConnection(db_username, db_password);
        String userHandle = bean.getUserHandle();
        String originalPassword = bean.getPassword();
        String sql = "SELECT * FROM users WHERE user_handle='" + userHandle + "'";
//        System.out.println(sql);
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(sql);
        if (rs.next()) {
            String storedPassword = rs.getString("password");
            boolean validatePassword = SecureHash.validatePassword(originalPassword, storedPassword);
            if (validatePassword) {
                bean.setFirstName(rs.getString("first_name"));
                bean.setLastName(rs.getString("last_name"));
                bean.setStatus(STATUS.getStatus(rs.getString("status")));
                bean.setValid(true);
            }
        } else {
            bean.setValid(false);
        }
//		rs.close();
//		st.close();
        con.close();
        return bean;
    }

    public static UserBean register(UserBean bean, String db_username, String db_password) throws ClassNotFoundException, SQLException, InvalidKeySpecException, NoSuchAlgorithmException {
        Connection con = ConnectionManager.getConnection(db_username, db_password);
        String userHandle = bean.getUserHandle();
        String sql = "SELECT user_handle FROM users WHERE user_handle='" + userHandle + "'";
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(sql);
        if (rs.next()) {
            bean.setValid(false);
        } else {
            sql = "INSERT INTO users(user_handle, first_name, last_name, password) VALUES(?,?,?,?)";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, bean.getUserHandle());
            pst.setString(2, bean.getFirstName());
            pst.setString(3, bean.getLastName());
            pst.setString(4, SecureHash.generateStorngPasswordHash(bean.getPassword()));
//			pst.setString(5, "user");
            pst.execute();
            bean.setValid(true);
        }
//		rs.close();
//		st.close();
        con.close();
        return bean;
    }

}

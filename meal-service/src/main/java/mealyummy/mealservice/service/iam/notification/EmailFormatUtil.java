package mealyummy.mealservice.service.iam.notification;

public class EmailFormatUtil {
    static String sendOtpInRegister = """
                <div style="background-color: #0b1006; color: #e0e4d2; font-family: 'Lexend', sans-serif; padding: 40px; border-radius: 16px; border: 1px solid #32362a; max-width: 600px; margin: 0 auto;">
                    <div style="text-align: center; margin-bottom: 30px;">
                        <h1 style="color: #ccff80; font-size: 32px; font-weight: 900; font-style: italic; margin: 0; text-shadow: 0 0 10px rgba(163,230,53,0.3);">MealYummy</h1>
                        <p style="color: #8c947c; font-size: 12px; letter-spacing: 2px; text-transform: uppercase; margin-top: 5px;">High-Performance Bio-Fuel System</p>
                    </div>
                    
                    <div style="background: rgba(26, 31, 46, 0.4); padding: 30px; border-radius: 12px; border: 1px solid rgba(255, 255, 255, 0.1); text-align: center;">
                        <h2 style="color: #ffffff; margin-bottom: 20px;">Xác thực tài khoản</h2>
                        <p style="color: #c2cab0; line-height: 1.6;">Để tiếp tục hành trình tối ưu hóa cơ thể, vui lòng sử dụng mã xác thực dưới đây:</p>
                        
                        <div style="background-color: #1d2116; color: #ccff80; font-size: 48px; font-weight: bold; letter-spacing: 10px; padding: 20px; border-radius: 8px; margin: 30px 0; border: 1px dashed #424936;">
                            %s
                        </div>
                        
                        <p style="color: #8c947c; font-size: 13px;">Mã xác thực có hiệu lực trong %d phút.</p>
                    </div>
                    
                    <div style="margin-top: 30px; text-align: center; border-top: 1px solid #32362a; padding-top: 20px;">
                        <p style="color: #c2cab0; font-size: 14px; font-style: italic;">"Your health is the ultimate performance."</p>
                        <p style="color: #424936; font-size: 11px; margin-top: 20px;">
                            © 2024 MealYummy Inc. | Hệ thống dinh dưỡng cá nhân hóa AI.<br/>
                            Nếu bạn không yêu cầu mã này, vui lòng bỏ qua.
                        </p>
                    </div>
                </div>
            """;

    static String sendOtpInForgotPassword = """
                <div style="background-color: #0b1006; color: #e0e4d2; font-family: 'Lexend', sans-serif; padding: 40px; border-radius: 16px; border: 1px solid #32362a; max-width: 600px; margin: 0 auto;">
                    <div style="text-align: center; margin-bottom: 30px;">
                        <h1 style="color: #ccff80; font-size: 32px; font-weight: 900; font-style: italic; margin: 0; text-shadow: 0 0 10px rgba(163,230,53,0.3);">MealYummy</h1>
                        <p style="color: #8c947c; font-size: 12px; letter-spacing: 2px; text-transform: uppercase; margin-top: 5px;">High-Performance Bio-Fuel System</p>
                    </div>
                    
                    <div style="background: rgba(26, 31, 46, 0.4); padding: 30px; border-radius: 12px; border: 1px solid rgba(255, 255, 255, 0.1); text-align: center;">
                        <h2 style="color: #ffffff; margin-bottom: 20px;">Quên mật khẩu</h2>
                        <p style="color: #c2cab0; line-height: 1.6;">Để tiếp tục hành trình tối ưu hóa cơ thể, vui lòng sử dụng mã xác thực dưới đây:</p>
                        
                        <div style="background-color: #1d2116; color: #ccff80; font-size: 48px; font-weight: bold; letter-spacing: 10px; padding: 20px; border-radius: 8px; margin: 30px 0; border: 1px dashed #424936;">
                            %s
                        </div>
                        
                        <p style="color: #8c947c; font-size: 13px;">Mã xác thực có hiệu lực trong %d phút.</p>
                    </div>
                    
                    <div style="margin-top: 30px; text-align: center; border-top: 1px solid #32362a; padding-top: 20px;">
                        <p style="color: #c2cab0; font-size: 14px; font-style: italic;">"Your health is the ultimate performance."</p>
                        <p style="color: #424936; font-size: 11px; margin-top: 20px;">
                            © 2024 MealYummy Inc. | Hệ thống dinh dưỡng cá nhân hóa AI.<br/>
                            Nếu bạn không yêu cầu mã này, vui lòng bỏ qua.
                        </p>
                    </div>
                </div>
            """;
}

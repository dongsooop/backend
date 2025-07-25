package com.dongsoop.dongsoop.mailverify.service;

import org.springframework.stereotype.Service;

@Service
public class MailTextGeneratorImpl implements MailTextGenerator {

    public String generateVerificationText(String code) {
        String staticForm = """
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>이메일 인증 코드 안내</title>
                    <style>
                        body {
                            font-family: 'Arial', sans-serif;
                            background-color: #f4f4f4;
                            margin: 0; padding: 0;
                        }
                        .container {
                            max-width: 600px;
                            margin: 40px auto;
                            background-color: #ffffff;
                            border-radius: 8px;
                            overflow: hidden;
                            box-shadow: 0 0 10px rgba(0,0,0,0.1);
                        }
                        .header {
                            background-color: #006DFF;
                            color: #ffffff;
                            text-align: center;
                            padding: 20px;
                        }
                        .header h1 {
                            margin: 0;
                            font-size: 24px;
                        }
                        .content-text {
                            padding: 30px;
                            color: #333333;
                            line-height: 1.6;
                        }
                        .code-box {
                            margin: 20px 0;
                            text-align: center;
                        }
                        .code {
                            margin-top: 20px;
                            display: inline-block;
                            background-color: #F0F0F0;
                            color: #006DFF;
                            font-size: 32px;
                            font-weight: bold;
                            padding: 12px 20px;
                            border-radius: 6px;
                            letter-spacing: 4px;
                        }
                        .footer {
                            background-color: #f4f4f4;
                            text-align: center;
                            font-size: 12px;
                            color: #888888;
                            padding: 15px;
                        }
                        a.button {
                            display: inline-block;
                            margin-top: 20px;
                            background-color: #006DFF;
                            color: #ffffff;
                            text-decoration: none;
                            padding: 10px 20px;
                            border-radius: 4px;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>동숲 이메일 인증 코드</h1>
                        </div>
                        <div class="code-box">
                            <span class="code">{{code}}</span>
                        </div>
                        <div class="content-text ">
                            <p>안녕하세요.</p>
                            <p>인증 코드를 가입 화면에 입력하여 본인 인증을 완료해 주세요.</p>
                            <p>만약 요청하지 않으셨다면, 이 메일을 무시하셔도 됩니다.</p>
                        </div>
                        <div class="footer">
                            &copy; 2025 Dongsoop. All rights reserved.<br>
                            문의: manager@dongsoop.site
                        </div>
                    </div>
                </body>
                </html>
                """;

        return staticForm.replace("{{code}}", code);
    }
}

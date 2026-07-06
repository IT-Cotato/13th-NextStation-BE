# 환승여행 BE

> **"매일 지나치던 지하철역을 오늘의 작은 여행지로"**
>
> 환승여행은 일상적인 출퇴근길이나 등하굣길의 지하철역을 매력적인 여행지로 재발견하도록 돕는 **역 기반 일상 여행 서비스**입니다.

<br>

## 👥 팀원 소개

<table align="center">
  <tr>
   <td align="center" width="200px">
      <a href="https://github.com/hyeonszz">
        <img src="https://github.com/hyeonszz.png" width="110px" style="border-radius: 50%;" alt="신현주 프로필"/><br />
        <br />
        <strong>신현주</strong>
      </a>
      <br>
      <small>13기 BE</small>
    </td>
   <td align="center" width="200dpx">
      <a href="https://github.com/leehwx">
        <img src="https://github.com/leehwx.png" width="110px" style="border-radius: 50%;" alt="이해원 프로필"/><br />
        <br />
        <strong>이해원</strong>
      </a>
      <br>
      <small>12기 BE</small>
    </td>
    <td align="center" width="200px">
      <a href="https://github.com/ch0iii">
        <img src="https://github.com/ch0iii.png" width="110px" style="border-radius: 50%;" alt="최정인 프로필"/><br />
        <br />
        <strong>최정인</strong>
      </a>
      <br>
      <small>13기 BE</small>
    </td>
  </tr>
</table>

## 📝 문서

<p align="left">
  <a href="https://rose-food-7e0.notion.site/Git-Convention-372ff5c94641800c84c2e480f01b26ab?source=copy_link"><img src="https://img.shields.io/badge/Git%20Convention-181717?style=for-the-badge&logo=git&logoColor=white"/></a>
  <a href="https://github.com/IT-Cotato/13th-NextStation-BE/blob/develop/docs/be-code-convention.md"><img src="https://img.shields.io/badge/Code%20Convention-0073EC?style=for-the-badge&logo=codeforces&logoColor=white"/></a>
</p>

<br>

## 🚀 로컬 개발환경 실행

### 사전 요구사항
* `Docker` / `Docker Compose`

### 실행 및 종료

프로젝트 루트 디렉토리에서 아래 명령어를 실행합니다.

```bash
# 로컬 컨테이너 환경 실행
docker compose -f docker-compose-local.yml up -d

# 로컬 컨테이너 환경 종료
docker compose -f docker-compose-local.yml down

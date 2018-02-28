1. 앱 이름 및 서버주소 변경

app>res>values>strings에서 "app_name"으로 앱 이름, "domain"으로 서버주소 변경


2. 액티비티 및 레이아웃 구성

Aa.java & activity_aa : 로그인 창
Ab.java & activity_ab : 웰컴창
Ac.java & activity_ac : 회원가입창
Ba.java & activity_ba : 녹음창
C.java & activity_c : 결과창(프래그먼트로 결과창 넣어야 함)

BarGraph & fragment_bar_graph : 바 그래프들로 이루어진 결과 화면(bar_graph_layout n개로 구성)
bar_graph_layout : 결과 하나 당 바 그래프의 레이아웃
years_layout : 회원가입 창의 출생연도 입력란 레이아웃


3. 녹음 결과 파라미터 수정

서버에서 튜플로 (예시: "{... 'result':'(1,2,3)'}" ) n개의 결과를 보내면, 맨 앞의 수치부터 순서대로 param_1, param_2, ... , param_n에 해당하는 항목으로 표시됩니다.
param_i의 항목명은 app>res>values>strings 에서 수정해야 합니다. 반드시 서버에서 보내는 결과값의 개수가 strings에서 정의한 param_i의 개수와 일치해야 합니다.
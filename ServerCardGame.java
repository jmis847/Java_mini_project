package project;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class ServerCardGame extends JFrame {
	JTextArea jta;
	JScrollPane jsp;
	ServerSocket ss;
	ArrayList<ConnServer> list = new ArrayList<ConnServer>();
	JTextField jtfScore;

	private int[] place;
	private int userTurnIndex = 0;

	public ServerCardGame() {
		// 화면 중앙에 창 키기
		setTitle("같은 그림 찾기 서버");
		Toolkit tool = Toolkit.getDefaultToolkit();
		Dimension d = tool.getScreenSize();

		double width = d.getWidth();
		double height = d.getHeight();

		int x = (int) (width / 2 - 400 / 2);
		int y = (int) (height / 2 - 600 / 2);

		jta = new JTextArea();
		jsp = new JScrollPane(jta, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		jsp.setBounds(940, 250, 200, 230);
		
		add(jsp);

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setBounds(x, y, 400, 600);
		setVisible(true);
		chatStart();
	}

	// 클라이언트 접속 메서드
	public void chatStart() {
		try {
			ss = new ServerSocket(5000);
			jta.append("서버 시작 \n");

			while (true) {
				jta.append("클라이언트 접속 대기중 \n");
				Socket client = ss.accept();
				// 클라이언트 접속하면 ip띄우기
				jta.append("ip : " + client.getInetAddress().getHostAddress() + "접속 \n");

				// 통신 담당 서버에게 접속한 클라이언트를 넘기기
				ConnServer cs = new ConnServer(client);
				list.add(cs); // 접속된 클라이언트를 ArrayList에 담기
				cs.start(); // 채팅 시작

			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// 통신을 담당할 서버
	class ConnServer extends Thread {
		Socket client;
		BufferedReader br;
		PrintWriter pw;
		String ip;

		ConnServer(Socket client) {
			this.client = client;

			// 1. 접속 사용자의 ip 출력
			ip = client.getInetAddress().getHostAddress();
			
			jta.append("현재 서버상태 : " + ip + "접속 \n"); // 접속시 서버창에 알림
			// 클라이언트가 접속할때 모든 클라이언트에게 알림
			broadcast(ip + " 님이 입장하였습니다");

			try {
				// 2. 수신부
				br = new BufferedReader(new InputStreamReader(client.getInputStream()));

				// 3. 발신부
				pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream())));

			} catch (IOException e) {
				e.printStackTrace();
			}

		}// ConnServer 생성자 end

		// 동시 처리할 코드 (여러 클라이언트와 통신 연결)
		@Override
		public void run() {
			// 4. 읽기 쓰기 반복
			try {
				while (true) {
					String msg = null;
					msg = br.readLine(); 
					
					String[] splitMsg = msg.split("/"); //클라이언트가 보낸 신호 받아서 쪼개기
					if(splitMsg.length > 1) {
						if(splitMsg[0].equals("StartGame")) { //start버튼신호 받아오면 카드섞고 뿌리기
							
							int gameSize = Integer.parseInt(splitMsg[1]);
							makePlace(gameSize);
							shuffle(gameSize);
							
							String strPlace = "StartMsg/";
							for(int i = 0; i < gameSize-1; i++) {
								strPlace += place[i] + "/";
							}
							strPlace += place[gameSize-1];
							broadcast(strPlace);
							setTurn();
						}
						else if (splitMsg[0].equals("ClickCard")) {  //클릭한 카드 서버에서 클라이언트들에게 전달
							broadcast("ClickCardIndex/" + splitMsg[1] + "/" + splitMsg[2]);
						}
						else if (splitMsg[0].equals("chat")) {  //채팅창에 로그인id랑 내용출력
							broadcast("chat/" + splitMsg[1] + "/" + splitMsg[2]);
						}
					}
					else if (msg.equals("nextTurn")) { //다음순서의 게임유저한테 턴전달
						System.out.println("이전 턴" + userTurnIndex);
						if(++userTurnIndex == list.size()) {
							userTurnIndex = 0;
						}
						setTurn();
						System.out.println("다음 턴" + userTurnIndex);
					}
					else {
						jta.append("[" + ip + "] : " + msg + "\n"); // ip와 함께 출력(서버창에)

						// 스크롤 제일 아래로 자동 설정
						jsp.getVerticalScrollBar().setValue(jsp.getVerticalScrollBar().getMaximum());
					}
				}
			} catch (IOException e) {
				broadcast(ip + " 님이 채팅방을 나갔습니다");
				list.remove(this);
				jta.append(ip + " 접속 종료 \n");
			}
		}

		public void broadcast(String msg) { //클라이언트한테 전달
			for (ConnServer x : list) {
				x.pw.println(msg);
				x.pw.flush();
			}
		}
		
		public void setTurn() { //게임유저 순서지정
			ConnServer conn = list.get(userTurnIndex);
			conn.pw.println("yourTurn");
			conn.pw.flush();
		}
	}

	private void makePlace(int gameSize) {
		place = new int[gameSize];
		for (int i = 0; i < gameSize; i++) {
			place[i] = i;
		}
	}

	private void shuffle(int gameSize) { // 섞기
		Random rnd = new Random();
		for (int i = 0; i < 1000; i++) {
			int num = rnd.nextInt(gameSize);

			// 섞기
			int temp = place[0];
			place[0] = place[num];
			place[num] = temp;
		}
	}
}

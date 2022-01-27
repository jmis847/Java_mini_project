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
		// ȭ�� �߾ӿ� â Ű��
		setTitle("���� �׸� ã�� ����");
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

	// Ŭ���̾�Ʈ ���� �޼���
	public void chatStart() {
		try {
			ss = new ServerSocket(5000);
			jta.append("���� ���� \n");

			while (true) {
				jta.append("Ŭ���̾�Ʈ ���� ����� \n");
				Socket client = ss.accept();
				// Ŭ���̾�Ʈ �����ϸ� ip����
				jta.append("ip : " + client.getInetAddress().getHostAddress() + "���� \n");

				// ��� ��� �������� ������ Ŭ���̾�Ʈ�� �ѱ��
				ConnServer cs = new ConnServer(client);
				list.add(cs); // ���ӵ� Ŭ���̾�Ʈ�� ArrayList�� ���
				cs.start(); // ä�� ����

			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// ����� ����� ����
	class ConnServer extends Thread {
		Socket client;
		BufferedReader br;
		PrintWriter pw;
		String ip;

		ConnServer(Socket client) {
			this.client = client;

			// 1. ���� ������� ip ���
			ip = client.getInetAddress().getHostAddress();
			
			jta.append("���� �������� : " + ip + "���� \n"); // ���ӽ� ����â�� �˸�
			// Ŭ���̾�Ʈ�� �����Ҷ� ��� Ŭ���̾�Ʈ���� �˸�
			broadcast(ip + " ���� �����Ͽ����ϴ�");

			try {
				// 2. ���ź�
				br = new BufferedReader(new InputStreamReader(client.getInputStream()));

				// 3. �߽ź�
				pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream())));

			} catch (IOException e) {
				e.printStackTrace();
			}

		}// ConnServer ������ end

		// ���� ó���� �ڵ� (���� Ŭ���̾�Ʈ�� ��� ����)
		@Override
		public void run() {
			// 4. �б� ���� �ݺ�
			try {
				while (true) {
					String msg = null;
					msg = br.readLine(); 
					
					String[] splitMsg = msg.split("/"); //Ŭ���̾�Ʈ�� ���� ��ȣ �޾Ƽ� �ɰ���
					if(splitMsg.length > 1) {
						if(splitMsg[0].equals("StartGame")) { //start��ư��ȣ �޾ƿ��� ī�弯�� �Ѹ���
							
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
						else if (splitMsg[0].equals("ClickCard")) {  //Ŭ���� ī�� �������� Ŭ���̾�Ʈ�鿡�� ����
							broadcast("ClickCardIndex/" + splitMsg[1] + "/" + splitMsg[2]);
						}
						else if (splitMsg[0].equals("chat")) {  //ä��â�� �α���id�� �������
							broadcast("chat/" + splitMsg[1] + "/" + splitMsg[2]);
						}
					}
					else if (msg.equals("nextTurn")) { //���������� ������������ ������
						System.out.println("���� ��" + userTurnIndex);
						if(++userTurnIndex == list.size()) {
							userTurnIndex = 0;
						}
						setTurn();
						System.out.println("���� ��" + userTurnIndex);
					}
					else {
						jta.append("[" + ip + "] : " + msg + "\n"); // ip�� �Բ� ���(����â��)

						// ��ũ�� ���� �Ʒ��� �ڵ� ����
						jsp.getVerticalScrollBar().setValue(jsp.getVerticalScrollBar().getMaximum());
					}
				}
			} catch (IOException e) {
				broadcast(ip + " ���� ä�ù��� �������ϴ�");
				list.remove(this);
				jta.append(ip + " ���� ���� \n");
			}
		}

		public void broadcast(String msg) { //Ŭ���̾�Ʈ���� ����
			for (ConnServer x : list) {
				x.pw.println(msg);
				x.pw.flush();
			}
		}
		
		public void setTurn() { //�������� ��������
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

	private void shuffle(int gameSize) { // ����
		Random rnd = new Random();
		for (int i = 0; i < 1000; i++) {
			int num = rnd.nextInt(gameSize);

			// ����
			int temp = place[0];
			place[0] = place[num];
			place[num] = temp;
		}
	}
}

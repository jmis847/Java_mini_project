package project;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.border.EtchedBorder;

import vo.CardVO;

public class CardGame extends JFrame implements ActionListener, Runnable, KeyListener, MouseListener {
	CardButton[] btn;
	JButton btnStart;
	JLabel lbPlayer1, lbPlayer2, lbScore1, lbScore2;
	JTextArea jta;
	EtchedBorder eborder;
	JPanel pnlCard, pnlScore;
	JScrollPane jsp;
	ImageIcon[] img;
	Socket s;
	BufferedReader br;
	PrintWriter pw;
	String[] images = { "icon01.png", "icon02.png", "icon03.png", "icon04.png", "icon05.png", "icon06.png",
			"icon07.png", "icon08.png", "icon09.png", "icon10.png", "icon11.png", "icon12.png", "icon13.png",
			"icon14.png", "icon15.png", "icon16.png", };

	Timer timer;
	JTextField jtf;
	int score = 0; // 점수
	int successCnt = 0; // 성공횟수

	private static final int gameSize = 32;
	private int[] place;
	private CardButton beforeButton = null;
	private CardButton afterButton = null;
	private boolean isChecked = false;
	private ImageIcon questionImg = null;
	private boolean myTurn = false;
	private boolean endTurn = false;
	private String Nickname = null; // 로그인하면 이름으로 지정하기

	public CardGame(String id) {
		Nickname = id;
		// 화면 중앙에 창 키기
		setTitle("같은 그림 찾기");
		Toolkit tool = Toolkit.getDefaultToolkit();
		Dimension d = tool.getScreenSize();

		double width = d.getWidth();
		double height = d.getHeight();

		int x = (int) (width / 2 - 1200 / 2);
		int y = (int) (height / 2 - 680 / 2);

		// 판넬, 버튼에 넣을 백그라운드 이미지 생성
		Image background = new ImageIcon(CardGame.class.getResource("../img/org_background.jpg")).getImage();
		Image start = new ImageIcon(CardGame.class.getResource("../img/btnStart.png")).getImage();
		// 컴포넌트 초기화
		pnlCard = setbackground(background);
		pnlScore = setbackground(background);
		lbPlayer1 = new JLabel("Turn");
		lbPlayer2 = new JLabel(Nickname, JLabel.CENTER);
		lbScore1 = new JLabel("Score");
		lbScore2 = new JLabel("0", JLabel.CENTER);
		btnStart = new JButton(){ // 버튼 이미지 넣기
			@Override
			protected void paintComponent(Graphics g) {
				g.drawImage(start, 0, 0, 200, 50, null);
			}
		};
		
		eborder = new EtchedBorder(Color.gray, Color.gray);

		jta = new JTextArea();
		jsp = new JScrollPane(jta, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		jtf = new JTextField();

		GridLayout grid = new GridLayout(4, 8);
		grid.setVgap(3);
		grid.setHgap(3);
		pnlCard.setLayout(grid);

		questionImg = imageSetSize("question.png");
		btn = new CardButton[gameSize]; // 게임사이즈(인원수)에 맞춰 버튼 개수 정하기
		for (int i = 0; i < btn.length; i++) { // 처음이미지를 question사진으로 배열사용해 넣어줌
			btn[i] = new CardButton();
			btn[i].setIcon(questionImg);
			btn[i].setIndex(i);

			btn[i].addMouseListener(this);
			pnlCard.add(btn[i]);
		}

		// 폰트설정
		Font f = new Font("고딕체", Font.BOLD, 20);

		lbPlayer1.setFont(f);
		lbPlayer2.setFont(f);
		lbScore1.setFont(f);
		lbScore2.setFont(f);
		lbPlayer1.setForeground(Color.white);
		lbPlayer2.setForeground(Color.white);
		lbScore1.setForeground(Color.white);
		lbScore2.setForeground(Color.white);

		lbPlayer2.setBorder(eborder);
		lbScore2.setBorder(eborder);

		pnlScore.setLayout(null);
		lbPlayer1.setBounds(1020, 20, 150, 30);
		lbPlayer2.setBounds(970, 60, 150, 50);
		lbScore1.setBounds(1020, 120, 150, 30);
		lbScore2.setBounds(970, 160, 150, 50);

		btnStart.setBounds(940, 550, 200, 50);

		pnlScore.setBounds(900, 0, 300, 640);
		pnlCard.setBounds(0, 0, 900, 640);

		jsp.setBounds(940, 250, 200, 210);
		jtf.setBounds(940, 480, 200, 20);

		add(lbPlayer1);
		add(lbPlayer2);
		add(lbScore1);
		add(lbScore2);
		add(btnStart);

		add(jsp);
		add(jtf);

		getContentPane().add(pnlCard);
		getContentPane().add(pnlScore);

		jtf.addKeyListener(this);
		btnStart.addActionListener(this);

		chatting();

		setResizable(false);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setBounds(x, y, 1200, 680);
		setVisible(true);

	}

	/**
	 * @param background
	 * @return
	 */
	// 판넬에 백그라운드 넣기
	public JPanel setbackground(Image background) {
		return new JPanel() {
			protected void paintComponent(Graphics g) {
				g.drawImage(background, 0, 0, 1200, 680, null);
			}
		};
	}

	// 이미지 사이즈 조절 메서드
	private ImageIcon imageSetSize(String filename) {
		ImageIcon icon = new ImageIcon("./src/img/" + filename);
		Image oi = icon.getImage();
		Image changedImage = oi.getScaledInstance(110, 160, Image.SCALE_SMOOTH);
		ImageIcon newIcon = new ImageIcon(changedImage);

		return newIcon;
	}

	public void setGame() {
		place = new int[gameSize];

		makePlace();

	}

	private void makePlace() {
		for (int i = 0; i < gameSize; i++) {
			place[i] = i;
		}
	}

	private void setVoToButton() {
		for (int i = 0; i < gameSize / 2; i++) {
			CardVO cardVO = new CardVO(imageSetSize(images[i]));
			btn[place[i * 2]].setCardVO(cardVO);
			btn[place[i * 2 + 1]].setCardVO(cardVO);
		}
	}

	// 짝이 안맞을시 다시 되돌리기
	public void back() {
		timer = new Timer(1000, new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
//				System.out.println("1초 후 뒤집기");

				afterButton.setIcon(questionImg);
				beforeButton.setIcon(questionImg);

				beforeButton = null;

				if (endTurn) {
					endTurn = false;
					sendMsg("nextTurn");
				}

				timer.stop();
			}
		});
		timer.start();
	}

	public void chatting() {
		Thread th = new Thread(this);
		th.start();
	}

	// 클라이언트가 동시에 실행할 코드
	@Override
	public void run() {
		try {
			s = new Socket("192.168.0.2", 5000);
			jta.append("서버 접속 완료 \n");

			// 서버와 통신
			br = new BufferedReader(new InputStreamReader(s.getInputStream()));
			pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(s.getOutputStream())));

			String msg = null;
			while ((msg = br.readLine()) != null) {
				String[] splitMsg = msg.split("/");

				if (splitMsg[0].equals("StartMsg")) { // 서버로 부터 받아와서 place에 저장
					for (int i = 0; i < gameSize; i++) {
						place[i] = Integer.parseInt(splitMsg[i + 1]);
					}
					setVoToButton();

					// 내가 누른게 서버로 부터 다시 돌아와서 중복되는 것을 방지(닉네임으로 구분)
				} else if (splitMsg[0].equals("ClickCardIndex")) {
					if (!Nickname.equals(splitMsg[1])) {
						clickEvent(btn[Integer.parseInt(splitMsg[2])]);
					}
				} else if (splitMsg[0].equals("chat")) {
					jta.append(splitMsg[1] + " : " + splitMsg[2] + "\n");
					// 스크롤 제일 아래로 설정
					jsp.getVerticalScrollBar().setValue(jsp.getVerticalScrollBar().getMaximum());
				} else if (msg.equals("yourTurn")) {
					myTurn = true;
					endTurn = true;
					jta.append("나의 턴입니다." + "\n");
					jsp.getVerticalScrollBar().setValue(jsp.getVerticalScrollBar().getMaximum());
				}

			}

		} catch (UnknownHostException e) {
			System.out.println("서버 ip 주소를 다시 확인하세요");
		} catch (IOException e) {
			System.out.println("접속 실패");
			e.printStackTrace();
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {

	}

	@Override
	public void keyPressed(KeyEvent e) {
		// 엔터 누르면 채팅 전송
		if (e.getKeyCode() == KeyEvent.VK_ENTER) {
			String msg = jtf.getText().trim();
			sendMsg("chat/" + Nickname + "/" + msg);
			jtf.setText("");
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (e.getSource() instanceof CardButton) {
			if (myTurn) {
				CardButton btn = (CardButton) e.getSource();
				clickEvent(btn);
				sendMsg("ClickCard/" + Nickname + "/" + btn.getIndex());
			}
		}
	}

	private void clickEvent(CardButton btn) {
		btn.setIcon(btn.getCardVO().getImage());

		if (!isChecked) { // 첫번째로 버튼 눌렀을 때
			beforeButton = btn;
			isChecked = true;
		} else { // 두번째 버튼 눌렀을 때
					// 두번째 버튼이 다른 버튼일 경우(동일버튼 클릭 방지)
			if (beforeButton.getIndex() != btn.getIndex()) {
				afterButton = btn;
				// 맞췄을 때
				if (beforeButton.getCardVO().equals(btn.getCardVO())) {
					System.out.println("정답");
					btn.removeMouseListener(this); // 지워서 맞추면 클릭 못하게 고정
					beforeButton.removeMouseListener(this);
					if (myTurn) {
						score += 10; // 점수증가 유저 점수 따로 주기
					}
					successCnt++; // 성공횟수증가

					lbScore2.setText(score + "");

					if (successCnt == gameSize / 2) { // 모든카드 다 뒤집으면 종료메세지
						JOptionPane.showConfirmDialog(this, "Game Over", "게임끝!", JOptionPane.PLAIN_MESSAGE);
//						sendMsg("endScore/" + score);
					}
				}
				// 틀렸을 때
				else { // 이전에 클릭한 버튼의 인덱스를 통해 다시 뒤집기
					myTurn = false;
					back();
				}
				isChecked = false;
			}
		}
	}

	private void sendMsg(String msg) {
		pw.println(msg);
		pw.flush();
	}

	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object obj = e.getSource();

		if (obj == btnStart) {
			System.out.println("start버튼 눌림");
			sendMsg("StartGame/" + gameSize);
		}
	}
}

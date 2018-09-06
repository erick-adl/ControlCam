#include <DNSServer.h>
#include <ESP8266WebServer.h>
#include <WiFiManager.h>
#include <PubSubClient.h>
#include <ESP8266WiFi.h>
#include <Servo.h>
#include <EEPROM.h>

WiFiServer server(80);
WiFiManager wifiManager;
WiFiClient espClient;
PubSubClient client(espClient);
Servo myservoX;
Servo myservoY;


const char* mqtt_server = "iot.eclipse.org";

long lastMsg = 0;
char msg[50];
int value = 0;

String ID_BOARD;

//SERVO COMMANDS
#define SERVO_X_INIT 50
#define SERVO_Y_INIT 150
#define MEM_ALOC_SIZE 24
static int posServoX = SERVO_X_INIT;
static int posServoY = SERVO_Y_INIT;
static int SaveposServoY = 0;
static int SaveposServoX = 0;
uint8_t SaveposServoX_eeprom;
uint8_t SaveposServoY_eeprom;
uint8_t SaveposServoRemember_eeprom;


//Topics
const char* topic_main = "ControlCamProject/";
const char* topic_status = "ControlCamProject/online-boards";

void setup() {

  /*CONFIG SERIAL*/
  Serial.begin(115200);
  delay(100);
  Serial.println("Starting...");




  /*CONFIG WIFI-MANAGER*/
  ID_BOARD = "CC" + String(ESP.getChipId());
  Serial.println(ID_BOARD);
  wifiManager.autoConnect(ID_BOARD.c_str());

  //if you get here you have connected to the WiFi
  Serial.print("Connected at: ");
  //  Serial.println(wifiManager.getSSID());
  Serial.print("IP address: ");
  Serial.println(WiFi.localIP());

  client.setServer(mqtt_server, 1883);
  client.setCallback(callback);

    //Config eeprom
  EEPROM.begin(MEM_ALOC_SIZE);  
  
  SaveposServoRemember_eeprom = EEPROM.read(2);
  Serial.printf("SaveposServoRemember: %d\n", SaveposServoRemember_eeprom);
  
  if(SaveposServoRemember_eeprom != 33){
    Serial.printf("Salvando primeiros dados...\n");
    EEPROM.write(2,33);
    EEPROM.write(0,SERVO_X_INIT);
    EEPROM.write(1,SERVO_Y_INIT);
  }else{
    Serial.printf("Lendo dados eeprom...\n");
    SaveposServoX_eeprom = EEPROM.read(0);
    SaveposServoY_eeprom = EEPROM.read(1);    
    Serial.printf("SaveposServoX: %d\n", SaveposServoX_eeprom);
    Serial.printf("SaveposServoY: %d\n", SaveposServoY_eeprom);
  }
    
  EEPROM.end();

  //  Configure Servo
  myservoX.attach(D3);
  myservoX.write(SaveposServoX_eeprom);

  myservoY.attach(D4);
  myservoY.write(SaveposServoY_eeprom);

}

void loop() {

  if (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.println("ControlCam desconectado da rede wi-fi");
    Serial.println("Reiniciando Sistema");
    ConfigWifiManager();
  }

  if (!client.connected()) {
    reconnect();
  }
  client.loop();

  long now = millis();
  if (now - lastMsg > 5000) {
    lastMsg = now;
    Serial.print("Publish message: ");
    Serial.println(ID_BOARD + "-online");
    client.publish(topic_status, ID_BOARD.c_str());
  }


}


void ConfigWifiManager()
{
  //exit after config instead of connecting
  wifiManager.setBreakAfterConfig(true);

  if (!wifiManager.autoConnect(ID_BOARD.c_str()))
  {
    Serial.println("Falha ao conectar.. Resetando sistema");
    delay(3000);
    ESP.reset();
    delay(5000);
  }

  //if you get here you have connected to the WiFi
  Serial.print("connected at: ");
  //  Serial.println(wifiManager.getSSID());
  Serial.println("IP address: ");
  Serial.println(WiFi.localIP());

}


void callback(char* topic, byte* payload, unsigned int length) {
  Serial.print("Message arrived in topic [");
  Serial.print(topic);
  Serial.print("] ");

  for (int i = 0; i < length; i++) {
    Serial.println((char)payload[i]);
  }

  if ((char)payload[0] == '2') {
    if (posServoX <= 180) {
      posServoX += 10;
      myservoX.write(posServoX);
      Serial.println(posServoX);
    }

  }
  if ((char)payload[0] == '1') {
    if (posServoX > 30) {
      posServoX -= 10;
      myservoX.write(posServoX);
      Serial.println(posServoX);
    }
  }
  if ((char)payload[0] == '4') {
    if (posServoY <= 140) {
      posServoY += 10;
      myservoY.write(posServoY);
      Serial.println(posServoY);
    }
  }

  if ((char)payload[0] == '3') {
    if (posServoY > 110) {
      posServoY -= 10;
      myservoY.write(posServoY);
      Serial.println(posServoY);
    }
  }

  if ((char)payload[0] == '5') {

    //Use save position
    posServoY = SaveposServoY;
    posServoX = SaveposServoX;


    myservoX.write(posServoX);
    myservoY.write(posServoY);
  }

  if ((char)payload[0] == '6') {
    //Save position of servos
    SaveposServoY = posServoY;
    SaveposServoX = posServoX;    
    //Salvando na eeprom
    EEPROM.begin(MEM_ALOC_SIZE);        ;
    EEPROM.write(0,posServoX);
    EEPROM.write(1,posServoY);     
    EEPROM.end();
  }

}

void reconnect() {
  // Loop until we're reconnected
  while (!client.connected()) {

    Serial.println("Attempting MQTT connection...");
    if (client.connect(ID_BOARD.c_str())) {
      Serial.println("Cliente " + ID_BOARD + " connected");

      // Once connected, publish an announcement...
      client.publish(topic_status, ID_BOARD.c_str());

      // ... and resubscribe
      String topic_command = "ControlCamProject/command/" + ID_BOARD;
      client.subscribe(topic_command.c_str());
    } else {
      Serial.print("failed, rc=");
      Serial.print(client.state());
      Serial.println(" try again in 5 seconds");
      // Wait 5 seconds before retrying
      delay(5000);
    }
  }
}

CREATE TABLE achievements (
    id          int not null AUTO_INCREMENT primary key,
    server_id   int not null,
    title       varchar(255) not null,
    description varchar(255) not null,
    score       int not null,
    reward      int not null default 0,
    type        varchar(32) not null,
    enabled     tinyint(1) not null default 1
);

CREATE TABLE player_achievements (
    id             int not null AUTO_INCREMENT primary key,
    achievement_id int not null,
    user_id        int not null,
    server_id      int not null,
    received       bigint not null,
    FOREIGN KEY (achievement_id) REFERENCES achievements(id) ON UPDATE CASCADE ON DELETE RESTRICT
);

INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Winner I', 'Win a match', 1, 25, 'WINS');
INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Winner II', 'Win 2 matches', 2, 50, 'WINS');
INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Winner III', 'Win 3 matches', 5, 75, 'WINS');

INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Winner IV', 'Win 5 matches', 5, 100, 'WINS');
INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Winner V', 'Win 10 matches', 5, 150, 'WINS');
INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Winner VI', 'Win 15 matches', 5, 200, 'WINS');

INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Winner VII', 'Win 20 matches', 5, 250, 'WINS');
INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Winner VIII', 'Win 30 matches', 5, 300, 'WINS');
INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Winner IX', 'Win 40 matches', 5, 350, 'WINS');

INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Winner X', 'Win 100 matches', 5, 400, 'WINS');
INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Winner XI', 'Win 100 matches', 5, 450, 'WINS');
INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Winner XII', 'Win 150 matches', 5, 500, 'WINS');

INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Winner XIII', 'Win 200 matches', 5, 550, 'WINS');
INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Winner XIV', 'Win 300 matches', 5, 600, 'WINS');
INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Winner XV', 'Win 400 matches', 5, 650, 'WINS');

INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Winner XVI', 'Win 500 matches', 5, 700, 'WINS');
INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Winner XVII', 'Win 700 matches', 5, 750, 'WINS');
INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Winner XVIII', 'Win 800 matches', 5, 800, 'WINS');

INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Winner XIX', 'Win 900 matches', 5, 900, 'WINS');
INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Winner XX', 'Win 1000 matches', 5, 1000, 'WINS');










INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Killer I', 'Kill an enemy', 1, 10, 'KILLS');
INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Killer II', 'Kill 5 enemies', 5, 20, 'KILLS');
INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Killer III', 'Kill 10 enemies', 10, 30, 'KILLS');

INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Killer IV', 'Kill 20 enemies', 10, 40, 'KILLS');
INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Killer V', 'Kill 30 enemies', 10, 50, 'KILLS');
INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Killer VI', 'Kill 40 enemies', 10, 60, 'KILLS');

INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Killer VII', 'Kill 50 enemies', 10, 70, 'KILLS');
INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Killer VIII', 'Kill 60 enemies', 10, 80, 'KILLS');
INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Killer IX', 'Kill 75 enemies', 10, 90, 'KILLS');

INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Killer X', 'Kill 100 enemies', 10, 100, 'KILLS');
INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Killer XI', 'Kill 200 enemies', 10, 125, 'KILLS');
INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Killer XII', 'Kill 300 enemies', 10, 150, 'KILLS');

INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Killer XIII', 'Kill 400 enemies', 10, 175, 'KILLS');
INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Killer XIV', 'Kill 500 enemies', 10, 200, 'KILLS');
INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Killer XV', 'Kill 600 enemies', 10, 225, 'KILLS');

INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Killer XVI', 'Kill 700 enemies', 10, 250, 'KILLS');
INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Killer XVII', 'Kill 800 enemies', 10, 275, 'KILLS');
INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Killer XVIII', 'Kill 900 enemies', 10, 300, 'KILLS');

INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Killer XIX', 'Kill 950 enemies', 10, 325, 'KILLS');
INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Killer XX', 'Kill 1000 enemies', 10, 350, 'KILLS');




INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Balancer I', 'Balance the teams 1 time', 1, 5, 'TEAM_BALANCER');
INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Balancer II', 'Balance the teams 2 times', 2, 10, 'TEAM_BALANCER');
INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Balancer III', 'Balance the teams 10 times', 10, 15, 'TEAM_BALANCER');
INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Balancer IV', 'Balance the teams 25 times', 10, 20, 'TEAM_BALANCER');
INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Balancer V', 'Balance the teams 50 times', 10, 25, 'TEAM_BALANCER');


INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Killstreak I', 'Get a killstreak of 2', 2, 5, 'KILLSTREAK');
INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Killstreak II', 'Get a killstreak of 5', 5, 10, 'KILLSTREAK');
INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Killstreak III', 'Get a killstreak of 10', 10, 50, 'KILLSTREAK');
INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Killstreak IV', 'Get a killstreak of 15', 10, 75, 'KILLSTREAK');
INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Killstreak V', 'Get a killstreak of 20', 10, 100, 'KILLSTREAK');

INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Killstreak VI', 'Get a killstreak of 30', 10, 200, 'KILLSTREAK');
INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Killstreak VII', 'Get a killstreak of 40', 10, 300, 'KILLSTREAK');
INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Killstreak VIII', 'Get a killstreak of 50', 10, 500,  'KILLSTREAK');
INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Killstreak IX', 'Get a killstreak of 75', 10, 750, 'KILLSTREAK');
INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Killstreak X', 'Get a killstreak of 100', 10, 1000, 'KILLSTREAK');




INSERT INTO achievements (server_id, title, description, score, reward, type)
    VALUES (6, 'Flawless', 'Win a match without any team deaths', 1, 50, 'FLAWLESS');


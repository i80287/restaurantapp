# restaurantapp

## Серверная часть

Для работы приложения необходимы: движка запуска контейнеров `Docker Engine` (можно установить вместе с `Docker Decktop`), `JVM` версии `>= 17` и наличие компилятора языка Kotlin версии `>= 1.9`


Для запуска серверной части приложения необходимо запустить службу/демона Docker и в консоли (или в IDE, которая поддерживает запуск) перейти в папку `backend`, 
чтобы файл `gradlew` находился на одном уровне с текущей папкой, и запустить команду:

В стандратном терминале `Windows`:

    .\gradlew bootRun -q --console=plain

В терминале `Unix` подобных систем:

    ./gradlew bootRun -q --console=plain

В случае `Unix` подобных систем, быть может, `git` по умолчанию склонирует файл без прав доступа на запуск. 
В таком случае, необходимо выдать это право, например, так:

    chmod +x gradlew

Также в системах `Linux` (возможно, и на `Mac OS`) `Docker` по умолчанию находится в отдельной группе, так что для запуска приложения (которое запустит контейнер с `postgresql` при помощи `docker-compose`) нужны будут права администратора. Подробное решение этой проблемы (чтобы не запускать сервер с правами адмнистратора), можно найти на сайте:

https://stackoverflow.com/questions/48957195/how-to-fix-docker-got-permission-denied-issue  


## Клиентская часть

Для выполнения функций из условия было написано клиентское консольное приложение, которое делает запросы к серверу (т.е. для работы всей системы нужен запущенный `docker`, сервер в папке `backend`, и хотя бы одно клиентское приложение в папке `interactor`)

Для его запуска необходимо перейти в папку `interactor` и выполнить команду:

В стандратном терминале `Windows`:

    .\gradlew bootRun -q --console=plain

В терминале `Unix` подобных систем:

    ./gradlew bootRun -q --console=plain

В случае `Unix` подобных систем, быть может, `git` по умолчанию склонирует файл без прав доступа на запуск. 
В таком случае, необходимо выдать это право, например, так:

    chmod +x gradlew

Приложение предложит ввести логин и пароль. По умолчанию в системе есть:

Пользователь с логином `user` и паролем `qwerty`

Администратор с логином `admin` и паролем `admin`

Администратор с логином `root` и паролем `root`

В соответствии с заданием, администраторы могут добавлять новых пользователей и удалять существующих

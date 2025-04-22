import axios from '/axios'

let RegisterUser ="http://localhost:8080/";

class RegisterUserService {
    getUserData() {
        return axios.get(RegisterUser);
    }
}
export default new RegisterUser();
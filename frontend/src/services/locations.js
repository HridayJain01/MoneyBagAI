/** Shared Indian state/city choices for customer and branch forms. */
define([], function () {
  'use strict';

  var citiesByState = {
    'Andhra Pradesh': ['Amaravati', 'Guntur', 'Tirupati', 'Vijayawada', 'Visakhapatnam'],
    'Arunachal Pradesh': ['Itanagar', 'Naharlagun', 'Pasighat'],
    'Assam': ['Dibrugarh', 'Dispur', 'Guwahati', 'Jorhat', 'Silchar'],
    'Bihar': ['Bhagalpur', 'Gaya', 'Muzaffarpur', 'Patna'],
    'Chhattisgarh': ['Bhilai', 'Bilaspur', 'Raipur'],
    'Goa': ['Margao', 'Panaji', 'Vasco da Gama'],
    'Gujarat': ['Ahmedabad', 'Gandhinagar', 'Rajkot', 'Surat', 'Vadodara'],
    'Haryana': ['Faridabad', 'Gurugram', 'Hisar', 'Panipat'],
    'Himachal Pradesh': ['Dharamshala', 'Shimla', 'Solan'],
    'Jharkhand': ['Bokaro', 'Dhanbad', 'Jamshedpur', 'Ranchi'],
    'Karnataka': ['Bengaluru', 'Hubballi', 'Mangaluru', 'Mysuru'],
    'Kerala': ['Kochi', 'Kollam', 'Kozhikode', 'Thiruvananthapuram', 'Thrissur'],
    'Madhya Pradesh': ['Bhopal', 'Gwalior', 'Indore', 'Jabalpur'],
    'Maharashtra': ['Mumbai', 'Nagpur', 'Nashik', 'Pune', 'Thane'],
    'Manipur': ['Imphal'],
    'Meghalaya': ['Shillong'],
    'Mizoram': ['Aizawl'],
    'Nagaland': ['Dimapur', 'Kohima'],
    'Odisha': ['Bhubaneswar', 'Cuttack', 'Puri', 'Rourkela'],
    'Punjab': ['Amritsar', 'Jalandhar', 'Ludhiana', 'Patiala'],
    'Rajasthan': ['Ajmer', 'Jaipur', 'Jodhpur', 'Kota', 'Udaipur'],
    'Sikkim': ['Gangtok'],
    'Tamil Nadu': ['Chennai', 'Coimbatore', 'Madurai', 'Salem', 'Tiruchirappalli'],
    'Telangana': ['Hyderabad', 'Karimnagar', 'Warangal'],
    'Tripura': ['Agartala'],
    'Uttar Pradesh': ['Agra', 'Kanpur', 'Lucknow', 'Noida', 'Varanasi'],
    'Uttarakhand': ['Dehradun', 'Haldwani', 'Haridwar'],
    'West Bengal': ['Asansol', 'Durgapur', 'Howrah', 'Kolkata', 'Siliguri'],
    'Andaman and Nicobar Islands': ['Port Blair'],
    'Chandigarh': ['Chandigarh'],
    'Dadra and Nagar Haveli and Daman and Diu': ['Daman', 'Silvassa'],
    'Delhi': ['New Delhi'],
    'Jammu and Kashmir': ['Jammu', 'Srinagar'],
    'Ladakh': ['Leh'],
    'Lakshadweep': ['Kavaratti'],
    'Puducherry': ['Puducherry']
  };

  return {
    states: Object.keys(citiesByState),
    citiesFor: function (state) {
      return citiesByState[state] || [];
    }
  };
});

#ifndef MDP_MODEL_HPP
#define MDP_MODEL_HPP

#include <string>
#include <iostream>
#include <array>

#include <nlohmann/json.hpp>

namespace MDPNameSpace
{
    using StateVector = std::array<size_t, 3>;
    using json = nlohmann::json;

    enum Actions
    {
        A_UP = 0,
        A_DOWN = 1,
        A_LEFT = 2,
        A_RIGHT = 3,
    };

    enum State
    {
        S_AGENT_PAN = 0,
        S_AGENT_TILT = 1,
        S_OBSERVATION = 2,
    };

    class Model
    {
    public:
        Model();

        size_t getS() const { return nStates; }
        size_t getA() const { return nActions; }
        double getDiscount() const { return discount; }

        double getTransitionProbability(size_t, size_t, size_t) const;
        double getExpectedReward(size_t, size_t, size_t) const;

        std::tuple<size_t, double> sampleSR(size_t, size_t) const;
        bool isTerminal(size_t) const;

        double getReward(StateVector, size_t, StateVector) const;
        double getTransition(const StateVector, size_t, const StateVector) const;

        StateVector decodeState(size_t) const;

        void setTarget(const size_t);

    private:
        const std::array<std::string, 4> actions = {"up", "down", "left", "right"};
        const std::array<std::string, 7> objects = {"Computer monitor", "Desk", "Window", "Kettle", "Sink", "Toilet", "Hand dryer"};

        const size_t gridSize = 6;
        const size_t blockSize = 1;

        const float discount = 0.95f;

        const size_t nActions = actions.size();
        const size_t nStates = gridSize * gridSize * objects.size();

        size_t target;

        json observationData = "{\"Desk\": {\"right\": {\"Desk\": 0.1323529411764706, \"Toilet\": 0.0029411764705882353, \"Computer monitor\": 0.6088235294117647, \"Hand dryer\": 0.0, \"Kettle\": 0.0029411764705882353, \"Sink\": 0.014705882352941176, \"Window\": 0.23823529411764705}, \"up\": {\"Desk\": 0.055762081784386616, \"Toilet\": 0.0, \"Computer monitor\": 0.7565055762081785, \"Hand dryer\": 0.0, \"Kettle\": 0.0, \"Sink\": 0.007434944237918215, \"Window\": 0.18029739776951673}, \"down\": {\"Desk\": 0.821917808219178, \"Toilet\": 0.0, \"Computer monitor\": 0.136986301369863, \"Hand dryer\": 0.0, \"Kettle\": 0.0, \"Sink\": 0.0273972602739726, \"Window\": 0.0136986301369863}, \"left\": {\"Desk\": 0.08241758241758242, \"Toilet\": 0.0, \"Computer monitor\": 0.7655677655677655, \"Hand dryer\": 0.0, \"Kettle\": 0.0, \"Sink\": 0.01098901098901099, \"Window\": 0.14102564102564102}}, \"Toilet\": {\"right\": {\"Desk\": 0.0, \"Toilet\": 0.2875816993464052, \"Computer monitor\": 0.0, \"Hand dryer\": 0.0, \"Kettle\": 0.0, \"Sink\": 0.6339869281045751, \"Window\": 0.0784313725490196}, \"up\": {\"Desk\": 0.0, \"Toilet\": 0.4059040590405904, \"Computer monitor\": 0.0, \"Hand dryer\": 0.0036900369003690036, \"Kettle\": 0.0, \"Sink\": 0.5202952029520295, \"Window\": 0.07011070110701106}, \"down\": {\"Desk\": 0.0, \"Toilet\": 0.9090909090909091, \"Computer monitor\": 0.0, \"Hand dryer\": 0.0, \"Kettle\": 0.0, \"Sink\": 0.08264462809917356, \"Window\": 0.008264462809917356}, \"left\": {\"Desk\": 0.0064516129032258064, \"Toilet\": 0.2838709677419355, \"Computer monitor\": 0.0, \"Hand dryer\": 0.0064516129032258064, \"Kettle\": 0.0, \"Sink\": 0.6774193548387096, \"Window\": 0.025806451612903226}}, \"Computer monitor\": {\"right\": {\"Desk\": 0.88, \"Toilet\": 0.0, \"Computer monitor\": 0.10947368421052632, \"Hand dryer\": 0.0, \"Kettle\": 0.0, \"Sink\": 0.0, \"Window\": 0.010526315789473684}, \"up\": {\"Desk\": 0.2777777777777778, \"Toilet\": 0.0, \"Computer monitor\": 0.4166666666666667, \"Hand dryer\": 0.0, \"Kettle\": 0.0, \"Sink\": 0.0, \"Window\": 0.3055555555555556}, \"down\": {\"Desk\": 0.980722891566265, \"Toilet\": 0.0, \"Computer monitor\": 0.018072289156626505, \"Hand dryer\": 0.0, \"Kettle\": 0.0, \"Sink\": 0.0012048192771084338, \"Window\": 0.0}, \"left\": {\"Desk\": 0.7695167286245354, \"Toilet\": 0.0, \"Computer monitor\": 0.19330855018587362, \"Hand dryer\": 0.0, \"Kettle\": 0.0, \"Sink\": 0.0037174721189591076, \"Window\": 0.03345724907063197}}, \"Hand dryer\": {\"right\": {\"Desk\": 0.0, \"Toilet\": 0.08333333333333333, \"Computer monitor\": 0.0, \"Hand dryer\": 0.4166666666666667, \"Kettle\": 0.0, \"Sink\": 0.5, \"Window\": 0.0}, \"up\": {\"Desk\": 0.0, \"Toilet\": 0.0, \"Computer monitor\": 0.0, \"Hand dryer\": 1.0, \"Kettle\": 0.0, \"Sink\": 0.0, \"Window\": 0.0}, \"down\": {\"Desk\": 0.0, \"Toilet\": 0.0625, \"Computer monitor\": 0.0, \"Hand dryer\": 0.1875, \"Kettle\": 0.0, \"Sink\": 0.75, \"Window\": 0.0}, \"left\": {\"Desk\": 0.0, \"Toilet\": 0.0, \"Computer monitor\": 0.0, \"Hand dryer\": 0.5, \"Kettle\": 0.0, \"Sink\": 0.5, \"Window\": 0.0}}, \"Kettle\": {\"right\": {\"Desk\": 0.0, \"Toilet\": 0.0, \"Computer monitor\": 0.0, \"Hand dryer\": 0.0, \"Kettle\": 0.9166666666666666, \"Sink\": 0.0, \"Window\": 0.08333333333333333}, \"up\": {\"Desk\": 0.0, \"Toilet\": 0.0, \"Computer monitor\": 0.0, \"Hand dryer\": 0.0, \"Kettle\": 0.8888888888888888, \"Sink\": 0.0, \"Window\": 0.1111111111111111}, \"down\": {\"Desk\": 0.0, \"Toilet\": 0.0, \"Computer monitor\": 0.0, \"Hand dryer\": 0.0, \"Kettle\": 1.0, \"Sink\": 0.0, \"Window\": 0.0}, \"left\": {\"Desk\": 0.043478260869565216, \"Toilet\": 0.0, \"Computer monitor\": 0.0, \"Hand dryer\": 0.0, \"Kettle\": 0.9565217391304348, \"Sink\": 0.0, \"Window\": 0.0}}, \"Sink\": {\"right\": {\"Desk\": 0.034482758620689655, \"Toilet\": 0.603448275862069, \"Computer monitor\": 0.005747126436781609, \"Hand dryer\": 0.028735632183908046, \"Kettle\": 0.0, \"Sink\": 0.1781609195402299, \"Window\": 0.14942528735632185}, \"up\": {\"Desk\": 0.010752688172043012, \"Toilet\": 0.053763440860215055, \"Computer monitor\": 0.005376344086021506, \"Hand dryer\": 0.06451612903225806, \"Kettle\": 0.0, \"Sink\": 0.24731182795698925, \"Window\": 0.6182795698924731}, \"down\": {\"Desk\": 0.041025641025641026, \"Toilet\": 0.7230769230769231, \"Computer monitor\": 0.0, \"Hand dryer\": 0.0, \"Kettle\": 0.0, \"Sink\": 0.2358974358974359, \"Window\": 0.0}, \"left\": {\"Desk\": 0.02512562814070352, \"Toilet\": 0.48743718592964824, \"Computer monitor\": 0.0, \"Hand dryer\": 0.03015075376884422, \"Kettle\": 0.0, \"Sink\": 0.15577889447236182, \"Window\": 0.3015075376884422}}, \"Window\": {\"right\": {\"Desk\": 0.11919504643962849, \"Toilet\": 0.006191950464396285, \"Computer monitor\": 0.01393188854489164, \"Hand dryer\": 0.0, \"Kettle\": 0.0, \"Sink\": 0.09287925696594428, \"Window\": 0.7678018575851393}, \"up\": {\"Desk\": 0.0026455026455026454, \"Toilet\": 0.0026455026455026454, \"Computer monitor\": 0.0, \"Hand dryer\": 0.0, \"Kettle\": 0.0, \"Sink\": 0.0, \"Window\": 0.9947089947089947}, \"down\": {\"Desk\": 0.2705718270571827, \"Toilet\": 0.026499302649930265, \"Computer monitor\": 0.015341701534170154, \"Hand dryer\": 0.0, \"Kettle\": 0.002789400278940028, \"Sink\": 0.1603905160390516, \"Window\": 0.5244072524407253}, \"left\": {\"Desk\": 0.1302250803858521, \"Toilet\": 0.01929260450160772, \"Computer monitor\": 0.008038585209003215, \"Hand dryer\": 0.0, \"Kettle\": 0.003215434083601286, \"Sink\": 0.04180064308681672, \"Window\": 0.797427652733119}}}"_json;
    };
}

#endif
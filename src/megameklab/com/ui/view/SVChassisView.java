/*
 * MegaMekLab
 * Copyright (C) 2019 The MegaMek Team
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package megameklab.com.ui.view;

import megamek.common.*;
import megamek.common.util.EncodeControl;
import megamek.common.verifier.TestSupportVehicle;
import megameklab.com.ui.util.CustomComboBox;
import megameklab.com.ui.util.TechComboBox;
import megameklab.com.ui.view.listeners.SVBuildListener;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Chassis panel for support vehicles
 */
public class SVChassisView extends BuildView implements ActionListener, ChangeListener {

    List<SVBuildListener> listeners = new CopyOnWriteArrayList<>();
    public void addListener(SVBuildListener l) {
        listeners.add(l);
    }
    public void removeListener(SVBuildListener l) {
        listeners.remove(l);
    }

    /** Subset of possible types that does not include those that are not yet supported */
    private final List<TestSupportVehicle.SVType> SV_TYPES = Arrays.stream(TestSupportVehicle.SVType.values())
            .filter(t -> !t.equals(TestSupportVehicle.SVType.AIRSHIP)
                    && !t.equals(TestSupportVehicle.SVType.RAIL)
                    && !t.equals(TestSupportVehicle.SVType.SATELLITE)).collect(Collectors.toList());
    private final Map<TestSupportVehicle.SVType, String> typeNames = new EnumMap<>(TestSupportVehicle.SVType.class);

    private final static TechAdvancement TA_DUAL_TURRET = Tank.getDualTurretTA();
    private final static TechAdvancement TA_CHIN_TURRET = VTOL.getChinTurretTA();

    private final SpinnerNumberModel spnTonnageModel = new SpinnerNumberModel(20.0, 5.0, 300.0, 0.5);
    private final SpinnerNumberModel spnTonnageModelSmall = new SpinnerNumberModel(4000.0, 100.0, 4999.0, 1.0);
    private final SpinnerNumberModel spnTurretWtModel = new SpinnerNumberModel(0.0, 0.0, null, 0.5);
    private final SpinnerNumberModel spnTurret2WtModel = new SpinnerNumberModel(0.0, 0.0, null, 0.5);
    private String[] turretNames;

    private final JSpinner spnTonnage = new JSpinner(spnTonnageModel);
    private final JCheckBox chkSmall = new JCheckBox();
    private final CustomComboBox<Integer> cbStructureTechRating = new CustomComboBox<>(ITechnology::getRatingName);
    private final CustomComboBox<TestSupportVehicle.SVType> cbType = new CustomComboBox<>(t -> typeNames.getOrDefault(t, "?"));
    private final TechComboBox<TestSupportVehicle.SVEngine> cbEngine = new TechComboBox<>(e -> e.engine.getEngineName()
            .replaceAll("^\\d+ ", ""));
    private final CustomComboBox<Integer> cbEngineTechRating = new CustomComboBox<>(ITechnology::getRatingName);
    private final CustomComboBox<Integer> cbTurrets = new CustomComboBox<>(i -> turretNames[i]);
    private final JSpinner spnChassisTurretWt = new JSpinner(spnTurretWtModel);
    private final JSpinner spnChassisTurret2Wt = new JSpinner(spnTurret2WtModel);

    /** List of components that should only be enabled for omnivehicles, to simplify enabling/disabling */
    private final List<JComponent> omniComponents = new ArrayList<>();

    private final ITechManager techManager;
    private boolean handleEvents = true;

    public SVChassisView(ITechManager techManager) {
        super();
        this.techManager = techManager;
        initUI();
    }

    private void initUI() {
        ResourceBundle resourceMap = ResourceBundle.getBundle("megameklab.resources.Views", new EncodeControl()); //$NON-NLS-1$
        for (TestSupportVehicle.SVType t : TestSupportVehicle.SVType.values()) {
            typeNames.put(t, resourceMap.getString("SVType." + t.toString()));
        }
        turretNames = resourceMap.getString("CVChassisView.turrets.values").split(","); //$NON-NLS-1$

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        add(createLabel(resourceMap.getString("SVChassisView.spnTonnage.text"), labelSize), gbc); //$NON-NLS-1$
        gbc.gridx = 1;
        setFieldSize(spnTonnage, spinnerSizeLg);
        spnTonnage.setToolTipText(resourceMap.getString("SVChassisView.spnTonnage.tooltip")); //$NON-NLS-1$
        add(spnTonnage, gbc);
        spnTonnage.addChangeListener(this);

        gbc.gridx = 2;
        chkSmall.setText(resourceMap.getString("SVChassisView.chkSmall.text")); //$NON-NLS-1$
        chkSmall.setToolTipText(resourceMap.getString("SVChassisView.chkSmall.tooltip")); //$NON-NLS-1$
        add(chkSmall, gbc);
        chkSmall.addActionListener(this);

        for (int r = ITechnology.RATING_A; r <= ITechnology.RATING_F; r++) {
            cbStructureTechRating.addItem(r);
        }
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.gridy++;
        add(createLabel(resourceMap.getString("SVChassisView.cbStructureTechRating.text"), labelSize), gbc); //$NON-NLS-1$
        gbc.gridx = 2;
        gbc.gridwidth = 1;
        setFieldSize(cbStructureTechRating, spinnerSize);
        cbStructureTechRating.setToolTipText(resourceMap.getString("SVChassisView.cbStructureTechRating.tooltip")); //$NON-NLS-1$
        add(cbStructureTechRating, gbc);
        cbStructureTechRating.addActionListener(this);

        SV_TYPES.forEach(cbType::addItem);
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.gridy++;
        add(createLabel(resourceMap.getString("SVChassisView.cbType.text"), labelSize), gbc); //$NON-NLS-1$
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        setFieldSize(cbType, controlSize);
        cbType.setToolTipText(resourceMap.getString("SVChassisView.cbType.tooltip")); //$NON-NLS-1$
        add(cbType, gbc);
        cbType.addActionListener(this);

        cbEngine.setModel(new DefaultComboBoxModel<>(TestSupportVehicle.SVEngine.values()));
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.gridy++;
        add(createLabel(resourceMap.getString("SVChassisView.cbEngine.text"), labelSize), gbc); //$NON-NLS-1$
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        setFieldSize(cbEngine, controlSize);
        cbEngine.setToolTipText(resourceMap.getString("SVChassisView.cbEngine.tooltip")); //$NON-NLS-1$
        add(cbEngine, gbc);
        cbEngine.addActionListener(this);

        for (int r = ITechnology.RATING_A; r <= ITechnology.RATING_F; r++) {
            cbEngineTechRating.addItem(r);
        }
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.gridy++;
        add(createLabel(resourceMap.getString("SVChassisView.cbEngineTechRating.text"), labelSize), gbc); //$NON-NLS-1$
        gbc.gridx = 2;
        gbc.gridwidth = 1;
        setFieldSize(cbEngineTechRating, spinnerSize);
        cbEngineTechRating.setToolTipText(resourceMap.getString("SVChassisView.cbEngineTechRating.tooltip")); //$NON-NLS-1$
        add(cbEngineTechRating, gbc);
        cbEngineTechRating.addActionListener(this);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        add(createLabel(resourceMap.getString("CVChassisView.cbTurrets.text"), labelSize), gbc); //$NON-NLS-1$
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        setFieldSize(cbTurrets, controlSize);
        cbTurrets.setToolTipText(resourceMap.getString("CVChassisView.cbTurrets.tooltip")); //$NON-NLS-1$
        add(cbTurrets, gbc);
        cbTurrets.addActionListener(this);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        add(new JLabel(resourceMap.getString("SVChassisView.lblBaseChassisTurretWeight.text")), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        JLabel lbl = createLabel(resourceMap.getString("SVChassisView.spnTurret1Wt.text"), labelSize); //$NON-NLS-1$
        add(lbl, gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        setFieldSize(spnChassisTurretWt, spinnerSize);
        spnChassisTurretWt.setToolTipText(resourceMap.getString("CVChassisView.spnTurretWt.tooltip")); //$NON-NLS-1$
        add(spnChassisTurretWt, gbc);
        spnChassisTurretWt.addChangeListener(this);
        omniComponents.add(lbl);
        omniComponents.add(spnChassisTurretWt);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        lbl = createLabel(resourceMap.getString("SVChassisView.spnTurret2Wt.text"), labelSize); //$NON-NLS-1$
        add(lbl, gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        setFieldSize(spnChassisTurret2Wt, spinnerSize);
        spnChassisTurret2Wt.setToolTipText(resourceMap.getString("CVChassisView.spnTurret2Wt.tooltip")); //$NON-NLS-1$
        add(spnChassisTurret2Wt, gbc);
        spnChassisTurret2Wt.addChangeListener(this);
        omniComponents.add(lbl);
        omniComponents.add(spnChassisTurret2Wt);
    }

    /**
     * @return The current weight value. Small support vees have the value converted from kg to tons.
     */
    public double getTonnage() {
        if (chkSmall.isSelected()) {
            return (double) spnTonnage.getValue() / 1000.0;
        } else {
            return (double) spnTonnage.getValue();
        }
    }

    /**
     * @return The currently selected support vehicle type
     */
    public TestSupportVehicle.SVType getType() {
        return (TestSupportVehicle.SVType) cbType.getSelectedItem();
    }

    /**
     * Used to check eligibility for turret(s).
     *
     * @return Whether the vehicle is an aerospace type
     */
    private boolean isAeroType() {
        final TestSupportVehicle.SVType type = getType();
        return type == TestSupportVehicle.SVType.FIXED_WING
                || type == TestSupportVehicle.SVType.AIRSHIP
                || type == TestSupportVehicle.SVType.SATELLITE;
    }

    /**
     * @return The currently selected structural tech rating, or A if none is selected.
     */
    private int getStructuralTechRating() {
        if (cbStructureTechRating.getSelectedItem() != null) {
            return (Integer) cbStructureTechRating.getSelectedItem();
        } else {
            return ITechnology.RATING_A;
        }
    }

    /**
     * @return The currently selected engine tech rating, or A if none is selected.
     */
    private int getEngineTechRating() {
        if (cbEngineTechRating.getSelectedItem() != null) {
            return (Integer) cbEngineTechRating.getSelectedItem();
        } else {
            return ITechnology.RATING_A;
        }
    }

    /**
     * @return The currently selected engine type, or {@link TestSupportVehicle.SVEngine#COMBUSTION COMBUSTION} if none is selected
     */
    private TestSupportVehicle.SVEngine getEngineType() {
        if (cbEngine.getSelectedItem() != null) {
            return (TestSupportVehicle.SVEngine) cbEngine.getSelectedItem();
        } else {
            return TestSupportVehicle.SVEngine.COMBUSTION;
        }
    }

    public void setFromEntity(Entity entity) {
        handleEvents = false;
        refresh();

        if (entity.getWeightClass() == EntityWeightClass.WEIGHT_SMALL_SUPPORT) {
            chkSmall.setSelected(true);
            spnTonnage.setModel(spnTonnageModelSmall);
            spnTonnageModelSmall.setValue(entity.getWeight() * 1000);
        } else {
            chkSmall.setSelected(false);
            spnTonnage.setModel(spnTonnageModel);
            spnTonnageModel.setValue(entity.getWeight());
        }
        cbEngine.removeAllItems();
        for (TestSupportVehicle.SVEngine engine : TestSupportVehicle.SVEngine.values()) {
            if (techManager.isLegal(engine)
                    && engine.isValidFor(entity)) {
                cbEngine.addItem(engine);
            }
        }
        cbEngine.setSelectedItem(TestSupportVehicle.SVEngine.getEngineType(entity.getEngine()));

        cbStructureTechRating.removeAllItems();
        for (int r = entity.getConstructionTechAdvancement().getTechRating(); r <= ITechnology.RATING_F; r++) {
            if (techManager.isLegal(TestSupportVehicle.TECH_LEVEL_TA[r])) {
                cbStructureTechRating.addItem(r);
            }
        }
        cbStructureTechRating.setSelectedItem(entity.getStructuralTechRating());

        cbType.setSelectedItem(TestSupportVehicle.SVType.getVehicleType(entity));

        // Engine::getTechRating will return the minimum tech rating for the engine type.
        cbEngineTechRating.removeAllItems();
        for (int r = entity.getEngine().getTechRating(); r <= ITechnology.RATING_F; r++) {
            if (techManager.isLegal(TestSupportVehicle.TECH_LEVEL_TA[r])) {
                cbEngineTechRating.addItem(r);
            }
        }
        cbEngineTechRating.setSelectedItem(entity.getEngineTechRating());

        handleEvents = true;
        // If the previously selected items are no longer valid, select the first in the list. This
        // is done with event handlers turned on so the Entity gets changed.
        if (getStructuralTechRating() != entity.getStructuralTechRating()) {
            cbStructureTechRating.setSelectedIndex(0);
        }
        if (getEngineTechRating() != entity.getEngineTechRating()) {
            cbEngine.setSelectedIndex(0);
        }

        if (entity instanceof Tank) {
            Tank tank = (Tank) entity;
            if (!tank.hasNoDualTurret()) {
                cbTurrets.setSelectedItem(SVBuildListener.TURRET_DUAL);
            } else if (!tank.hasNoTurret()) {
                cbTurrets.setSelectedItem((tank.getMovementMode() == EntityMovementMode.VTOL) ?
                        SVBuildListener.TURRET_CHIN : SVBuildListener.TURRET_SINGLE);
            } else {
                cbTurrets.setSelectedItem(SVBuildListener.TURRET_NONE);
            }
            spnTurretWtModel.setValue(Math.max(0, tank.getBaseChassisTurretWeight()));
            spnTurret2WtModel.setValue(Math.max(0, tank.getBaseChassisTurret2Weight()));
            cbTurrets.setEnabled(true);
        } else {
            cbTurrets.setEnabled(false);
        }

        omniComponents.forEach(c -> c.setEnabled(entity.isOmni()));
        spnChassisTurretWt.setEnabled(!entity.isAero() && entity.isOmni() && (cbTurrets.getSelectedIndex() > 0));
        spnChassisTurret2Wt.setEnabled(!entity.isAero() && entity.isOmni() && (cbTurrets.getSelectedIndex() > 1));
    }

    public void refresh() {
        refreshTonnage();
        refreshTurrets();
    }

    private void refreshTonnage() {
        double max = getType().maxTonnage;
        spnTonnageModel.setMaximum(max);
        if (spnTonnageModel.getNumber().doubleValue() > max) {
            spnTonnageModel.setValue(max);
        }
    }

    private void refreshTurrets() {
        Integer prev = (Integer) cbTurrets.getSelectedItem();
        handleEvents = false;
        cbTurrets.removeAllItems();
        cbTurrets.addItem(SVBuildListener.TURRET_NONE);
        if (TestSupportVehicle.SVType.VTOL.equals(cbType.getSelectedItem())) {
            if (techManager.isLegal(TA_CHIN_TURRET)) {
                cbTurrets.addItem(SVBuildListener.TURRET_CHIN);
            }
        } else if (!isAeroType()) {
            cbTurrets.addItem(SVBuildListener.TURRET_SINGLE);
            if (techManager.isLegal(TA_DUAL_TURRET)) {
                cbTurrets.addItem(SVBuildListener.TURRET_DUAL);
            }
        }
        cbTurrets.setSelectedItem(prev);
        handleEvents = true;
        if (cbTurrets.getSelectedIndex() < 0) {
            cbTurrets.setSelectedIndex(0);
        }
        double maxWt = getTonnage();
        spnTurretWtModel.setMaximum(maxWt);
        if (spnTurretWtModel.getNumber().doubleValue() > maxWt) {
            spnTurretWtModel.removeChangeListener(this);
            spnTurretWtModel.setValue(maxWt);
            spnTurretWtModel.addChangeListener(this);
        }
        spnTurret2WtModel.setMaximum(maxWt);
        if (spnTurret2WtModel.getNumber().doubleValue() > maxWt) {
            spnTurret2WtModel.removeChangeListener(this);
            spnTurret2WtModel.setValue(maxWt);
            spnTurret2WtModel.addChangeListener(this);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!handleEvents) {
            return;
        }
        if (e.getSource() == chkSmall) {
            spnTonnage.setModel(chkSmall.isSelected() ?
                spnTonnageModelSmall : spnTonnageModel);
            listeners.forEach(l -> l.tonnageChanged(getTonnage()));
        } else if (e.getSource() == cbType) {
            listeners.forEach(l -> l.typeChanged(getType()));
        } else if (e.getSource() == cbStructureTechRating) {
            listeners.forEach(l -> l.structuralTechRatingChanged(getStructuralTechRating()));
        } else if (e.getSource() == cbEngine) {
            listeners.forEach(l -> l.engineChanged(getEngineType().engine));
        } else if (e.getSource() == cbEngineTechRating) {
            listeners.forEach(l -> l.engineTechRatingChanged(getEngineTechRating()));
        } else if (e.getSource() == cbTurrets) {
            if (cbTurrets.getSelectedItem() != null) {
                listeners.forEach(l -> l.turretChanged((Integer) cbTurrets.getSelectedItem()));
            }
        }
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (!handleEvents) {
            return;
        }
        if (e.getSource() == spnTonnage) {
            listeners.forEach(l -> l.tonnageChanged(getTonnage()));
        } else if ((e.getSource() == spnChassisTurretWt)
                || (e.getSource() == spnChassisTurret2Wt)){
            listeners.forEach(l -> l.turretBaseWtChanged(spnTurretWtModel.getNumber().doubleValue(),
                    spnTurret2WtModel.getNumber().doubleValue()));
        }
    }
}
